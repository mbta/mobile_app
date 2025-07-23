#!/usr/bin/env ruby
# frozen_string_literal: true

require 'yaml'

module DiffCrawler
  refine Hash do
    def crawl!(path = '')
      if leaf?
        replace_leaf_keys!
        ignore_leaf_noise!(path)
      else
        crawl_recursive!(path)
        ignore_file_type_becoming_explicit!(path)
      end
    end

    def leaf?
      (keys.size == 2) && (keys.one? { |el| el.include?('diff-xcodeproj.sh') })
    end

    def replace_leaf_keys!
      dirty_key = keys.find { |el| !el.include?('diff-xcodeproj.sh') }
      gen_key = keys.find { |el| el.include?('diff-xcodeproj.sh') }
      self['dirty'] = delete(dirty_key)
      self['generated'] = delete(gen_key)
    end

    def ignore_leaf_noise!(path)
      ignore_added_file_type!(path)
      ignore_object_version!(path)
      ignore_pods_name!(path)
      ignore_pods_frameworks!(path)
    end

    def ignore_added_file_type!(path)
      boring_file_types = ['text', 'text.xml', 'file']
      if path.end_with?('lastKnownFileType') && boring_file_types.include?(self['dirty']) && self['generated'].nil?
        replace({})
      end
    end

    def ignore_object_version!(path)
      return unless path == '.objectVersion'

      replace({})
    end

    def ignore_pods_name!(path)
      if (path == '.rootObject.mainGroup.children.Pods.name') && self['dirty'].nil? && (self['generated'] == 'Pods')
        replace({})
      end
    end

    def ignore_pods_frameworks!(path)
      return unless path.include?('Embed Pods Frameworks') && self['dirty'] == [] && self['generated'].nil?

      replace({})
    end

    def crawl_recursive!(path)
      keys.each do |k|
        next unless self[k].is_a? Hash

        self[k].crawl!("#{path}.#{k}")
        delete(k) if self[k].empty?
      end
    end

    def ignore_file_type_becoming_explicit!(_path)
      explicit = self['explicitFileType'] || return
      last_known = self['lastKnownFileType'] || return
      unless explicit['dirty'] == last_known['generated'] && explicit['generated'].nil? && last_known['dirty'].nil?
        return
      end

      delete('explicitFileType')
      delete('lastKnownFileType')
    end
  end
end

using DiffCrawler

data = YAML.safe_load_file(ARGV[0] || raise('usage: filter-xcodeproj-diff.rb path/to/diff.yml')) || {}

data.crawl!

puts(YAML.dump(data).sub("---\n", '')) unless data.empty?

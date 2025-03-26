#!/usr/bin/env ruby

require 'yaml'

module DiffCrawler
  refine Hash do
    def crawl!(path = "")
      if self.keys.size == 2 and self.keys.count {|el| el.include?("diff-xcodeproj.sh")} == 1
        dirty_key = self.keys.find {|el| not el.include?("diff-xcodeproj.sh")}
        gen_key = self.keys.find {|el| el.include?("diff-xcodeproj.sh")}
        self["dirty"] = self.delete(dirty_key)
        self["generated"] = self.delete(gen_key)
        if path.end_with?("lastKnownFileType") and ["text", "text.xml", "file"].include?(self["dirty"]) and self["generated"] == nil
          self.replace({})
        end
        if path == ".objectVersion"
          self.replace({})
        end
        if path == ".rootObject.mainGroup.children.Pods.name" and self["dirty"] == nil and self["generated"] == "Pods"
          self.replace({})
        end
      else
        for k in self.keys
          if self[k].is_a? Hash
            self[k].crawl!(path + "." + k)
            if self[k].empty?
              self.delete(k)
            end
          end
        end
        if (self["explicitFileType"]&.fetch("dirty") || :nil_and_shouldnt_count) == self["lastKnownFileType"]&.fetch("generated") and self["explicitFileType"]["generated"] == nil and self["lastKnownFileType"]["dirty"] == nil
          self.delete("explicitFileType")
          self.delete("lastKnownFileType")
        end
      end
    end
  end
end

using DiffCrawler

data = YAML.safe_load_file(ARGV[0] || fail("usage: filter-xcodeproj-diff.rb path/to/diff.yml")) || {}

data.crawl!

unless data.empty?
  puts(YAML.dump(data).sub("---\n", ""))
end

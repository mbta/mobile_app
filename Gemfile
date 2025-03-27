# frozen_string_literal: true

source 'https://rubygems.org'

gem 'cocoapods'
gem 'cyclonedx-cocoapods'
gem 'fastlane'
gem 'rubocop', require: false
gem 'xcodeproj'

plugins_path = File.join(File.dirname(__FILE__), 'fastlane', 'Pluginfile')
eval_gemfile(plugins_path) if File.exist?(plugins_path)

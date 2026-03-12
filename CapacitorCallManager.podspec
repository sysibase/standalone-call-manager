require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

Pod::Spec.new do |s|
  s.name = 'CapacitorCallManager'
  s.version = package['version']
  s.summary = package['description']
  s.license = package['license']
  s.homepage = 'https://github.com/ibase/capacitor-call-manager'
  s.author = package['author']
  s.source = { :git => 'https://github.com/ibase/capacitor-call-manager', :tag => s.version.to_s }
  s.source_files = 'ios/Sources/CallManagerPlugin/**/*.{swift,h,m,c,cc,mm,cpp}'
  s.ios.deployment_target  = '13.0'
  s.dependency 'Capacitor'
  s.static_framework = true
  s.swift_version = '5.1'
end

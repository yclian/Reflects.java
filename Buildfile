repositories.remote << 'http://repo1.maven.org/maven2/'

GUAVA = transitive('com.google.guava:guava:jar:10.0.1')
SLF4J = ['org.slf4j:slf4j-api:jar:1.6.1'] << transitive('org.slf4j:slf4j-log4j12:jar:1.6.1')
JUNIT = 'junit:junit:jar:4.9'
MOCKITO = 'org.mockito:mockito-all:jar:1.9.0-rc1'

desc 'Awesome reflection utilities'
define 'reflects' do
  project.version = '1.0'
  project.group = 'my.jug.reflect'
  compile.options.target = '1.7'
  compile.options.target = '1.6'
  compile.with GUAVA, SLF4J
  test.with JUNIT, MOCKITO
end

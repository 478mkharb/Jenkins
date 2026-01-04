// Create folders
folder('demo1') {
    description('Parent folder')
}

folder('demo1/demo2') {
    description('Nested folder under test1')
}

def sharedWs = '/home/jenkins/shared-workspace'

// Job inside test1
job('demo1/job1') {
    description('Freestyle job1 inside test1 folder')

    customWorkspace(sharedWs)
    concurrentBuild(false)

    scm {
        git {
            remote {
                url('https://github.com/opstree/spring3hibernate.git')
            }
            branch('*/master')
        }
    }

    steps {
        shell('echo "Running test1/job1"')
    }
}

// Job inside test1/test2
job('demo1/demo2/job2') {
    description('Freestyle job2 inside test1/test2 folder')

    customWorkspace(sharedWs)
    concurrentBuild(false)

    scm {
        git {
            remote {
                url('https://github.com/478mkharb/Jenkins.git')
            }
            branch('*/main')
        }
    }

    steps {
        shell('echo "Running test1/test2/job2"')
    }
}

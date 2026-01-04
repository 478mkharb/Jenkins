folder('demo1') {
    description('Parent folder')
}

folder('demo1/demo2') {
    description('Nested folder')
}

def sharedWs = '/home/jenkins/java-shared-wspace'

job('demo1/job1') {
    description('Checkout Java project source code')

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
        shell('echo "Running demo1/job1"')
    }
}

job('demo1/demo2/job2') {
    description('Build using source from job1 workspace')

    customWorkspace(sharedWs)
    concurrentBuild(false)

    steps {
        shell('mvn clean package -DskipTests')
    }
}

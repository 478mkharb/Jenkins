// Create folders
folder('demo1') {
    description('Parent folder')
}

folder('demo1/demo2') {
    description('Nested folder under demo1')
}

def sharedWs = '/home/jenkins/java-shared-wspace'

// Job 1: Checkout source code
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
        shell('''
            echo "Source code checked out"
            ls -l
        ''')
    }
}

// Job 2: Build code checked out by job1
job('demo1/demo2/job2') {
    description('Build using Maven from shared workspace')

    customWorkspace(sharedWs)
    concurrentBuild(false)

    steps {
        shell('''
            echo "Running Maven build from shared workspace"
            pwd
            mvn clean package -DskipTests
        ''')
    }
}

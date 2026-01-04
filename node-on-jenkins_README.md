# Jenkins SSH Agents on AWS EC2 (Ubuntu)

This document is a **complete, step-by-step record** of configuring **two Ubuntu EC2 instances as Jenkins nodes (agents) via SSH** and using them in **Jenkins Pipeline jobs**.

This README is written so that:

* You can **reproduce the setup anytime**
* You can **debug issues quickly**
* You can **use it in interviews, assignments, or production reference**

---

## 📌 Architecture Overview

```
Jenkins Master (Ubuntu 24)
   |
   |-- SSH (jenkins_key)
   |        |
   |        |--> EC2 Agent 1 (Ubuntu)
   |
   |-- SSH (jenkins_key)
            |
            |--> EC2 Agent 2 (Ubuntu)
```

---

## 🧱 Environment Details

### Jenkins Master

* OS: Ubuntu 24
* Jenkins user: `jenkins`
* Jenkins installed via apt
* Java: OpenJDK 17

### Jenkins Agents (EC2)

* OS: Ubuntu EC2
* User: `ubuntu`
* Java: OpenJDK 17
* Authentication: SSH key-based

---

## 🔐 SSH Keys Used (Important Concept)

| Key           | Purpose                                  |
| ------------- | ---------------------------------------- |
| `server3.pem` | AWS key pair – **bootstrap access only** |
| `jenkins_key` | Permanent SSH key used by Jenkins        |
| `id_ed25519`  | Default system key (not used)            |

> ⚠️ AWS `.pem` keys are **NOT** used directly by Jenkins agents.

---

## 1️⃣ Prerequisites

### On Jenkins Master

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk openssh-client
```

Verify:

```bash
java -version
```

---

### On Each EC2 Instance

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk openssh-server
```

Verify:

```bash
java -version
```

---

## 2️⃣ Create Jenkins SSH Key (One Time)

Run as **jenkins user** on Jenkins master:

```bash
sudo su - jenkins
ssh-keygen -t rsa -b 4096 -f ~/.ssh/jenkins_key
```

Generated files:

```
/var/lib/jenkins/.ssh/jenkins_key
/var/lib/jenkins/.ssh/jenkins_key.pub
```

Permissions (must be):

```
-rw------- jenkins_key
-rw-r--r-- jenkins_key.pub
```

---

## 3️⃣ Bootstrap Login to EC2 Using AWS Key

This step is **only for initial access**.

```bash
ssh -i server3.pem ubuntu@<EC2_IP_1>
ssh -i server3.pem ubuntu@<EC2_IP_2>
```

If this fails, check:

* Correct username (`ubuntu` for Ubuntu AMI)
* Correct key pair attached to EC2
* Security Group allows SSH (port 22)

---

## 4️⃣ Install Jenkins Public Key on EC2

### On Jenkins Master

```bash
cat /var/lib/jenkins/.ssh/jenkins_key.pub
```

Copy the entire output.

---

### On Each EC2 Instance

```bash
mkdir -p ~/.ssh
chmod 700 ~/.ssh
nano ~/.ssh/authorized_keys
```

Paste the Jenkins public key on a new line.

```bash
chmod 600 ~/.ssh/authorized_keys
```

Verify:

```bash
ls -ld ~/.ssh
ls -l ~/.ssh/authorized_keys
```

Expected:

```
drwx------ ubuntu ubuntu .ssh
-rw------- ubuntu ubuntu authorized_keys
```

---

## 5️⃣ Verify Jenkins SSH Access (Mandatory Test)

From Jenkins master:

```bash
ssh -i /var/lib/jenkins/.ssh/jenkins_key ubuntu@<EC2_IP_1>
ssh -i /var/lib/jenkins/.ssh/jenkins_key ubuntu@<EC2_IP_2>
```

✅ Must login **without password**

If this fails → Jenkins node will fail.

---

## 6️⃣ Add SSH Credentials in Jenkins

Jenkins UI → **Manage Jenkins → Credentials → Global → Add Credentials**

* Kind: `SSH Username with private key`
* Username: `ubuntu`
* Private Key: paste `jenkins_key`
* ID: `ec2-ssh-key`
* Description: `Ubuntu EC2 SSH Key`

Save.

---

## 7️⃣ Create Jenkins Nodes

### Node: ec2-agent-1

* Name: `ec2-agent-1`
* Type: Permanent Agent
* Remote root directory:

  ```
  /home/ubuntu/jenkins
  ```
* Labels:

  ```
  ec2 ubuntu agent1
  ```
* Launch method: **Launch agent via SSH**
* Host: `<EC2_IP_1>`
* Credentials: `ec2-ssh-key`
* Host Key Verification: **Non-verifying**

Save.

---

### Node: ec2-agent-2

Same as above, except:

* Name: `ec2-agent-2`
* Host: `<EC2_IP_2>`
* Labels:

  ```
  ec2 ubuntu agent2
  ```

---

## 8️⃣ Fix: Remote Root Directory Must Exist

On each EC2:

```bash
mkdir -p /home/ubuntu/jenkins
chown ubuntu:ubuntu /home/ubuntu/jenkins
chmod 755 /home/ubuntu/jenkins
```

> ⚠️ A typo like `/hone/ubuntu/jenkins` will cause agent launch failure.

---

## 9️⃣ Launch and Verify Agents

Jenkins UI:

```
Manage Jenkins → Nodes → ec2-agent-* → Launch agent
```

Expected state:

* Status: **ONLINE**
* Executor: `1/1`

---

## 🔟 Jenkins Pipeline Usage

### Sequential Example

```groovy
pipeline {
    agent none
    stages {
        stage('Run on Agent 1') {
            agent { label 'agent1' }
            steps {
                sh 'hostname'
            }
        }
        stage('Run on Agent 2') {
            agent { label 'agent2' }
            steps {
                sh 'hostname'
            }
        }
    }
}
```

---

### Parallel Example (Recommended)

```groovy
pipeline {
    agent none
    stages {
        stage('Parallel Jobs') {
            parallel {
                stage('Build') {
                    agent { label 'agent1' }
                    steps {
                        sh 'echo Build on Agent 1'
                    }
                }
                stage('Test') {
                    agent { label 'agent2' }
                    steps {
                        sh 'echo Test on Agent 2'
                    }
                }
            }
        }
    }
}
```

---

## 🛠 Common Errors & Fixes

### Agent Offline – Failed to launch agent process

* Java missing → install OpenJDK
* Remote root directory typo
* Wrong permissions on `/home/ubuntu/jenkins`

---

### Permission denied (publickey)

* Wrong username (`ec2-user` vs `ubuntu`)
* Jenkins public key not in `authorized_keys`
* Incorrect file permissions

---

### ssh-copy-id fails on EC2

This is expected.

> EC2 disables password authentication. Always install Jenkins key **manually**.

---

## 🧠 Interview-Ready Summary

> Jenkins agents are connected via SSH using a dedicated Jenkins key. AWS key pairs are used only for initial access. Each agent must have Java installed and a writable remote root directory. Pipelines use node labels, not node names.

---

**End of Record**

### Full Setup of Docker Agent for Jenkins (Cloud) on Ubuntu VM

This guide assumes you have a Jenkins controller running at `192.168.0.100` and a fresh Ubuntu VM at `192.168.81.128` for Docker agents.

---

## Step 0: Prepare Ubuntu VM

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y curl wget git ufw
```

Ensure you have sudo access and VM can reach Jenkins controller (192.168.0.100).

---

## Step 1: Install Docker Engine on Ubuntu VM

```bash
# Install prerequisites
sudo apt install -y ca-certificates curl gnupg lsb-release

# Add Docker’s official GPG key
sudo mkdir -p /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg

# Set up Docker repository
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker Engine
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Enable and start Docker
sudo systemctl enable docker
sudo systemctl start docker

# Test Docker
docker run hello-world
```

---

## Step 2: Configure Firewall (if using UFW)

```bash
sudo ufw allow ssh
sudo ufw allow from 192.168.0.100 to any port 2375  # optional if using TCP socket
sudo ufw enable
```

---

## Step 3: Create Jenkins Cloud on Controller

1. Go to **Jenkins Controller UI → Manage Jenkins → Configure Clouds → Add a new cloud → Docker**
2. **Docker Host URI:**

   * If using Unix socket: `unix:///var/run/docker.sock`
   * If using TCP socket: `tcp://192.168.81.128:2375`
3. Test connection → should succeed.

---

## Step 4: Add Docker Agent Template

1. **Agent template name:** `docker-agent`
2. **Labels:** `docker-agent`
3. **Docker image:** `jenkins/inbound-agent:latest`
4. **Remote FS root:** `/home/jenkins`
5. **Launch method:** `Launch agent by connecting it to the master` (JNLP)
6. **Jenkins URL:** `http://192.168.0.100:8080`
7. **JNLP port:** 50000 (Fixed)
8. Optional environment variables:

   ```text
   JENKINS_AGENT_WORKDIR=/home/jenkins
   ```
9. Limit number of executors per agent to 1 or 2 depending on VM capacity.

---

## Step 5: Configure Jenkins Controller for Agents

1. **Manage Jenkins → Configure Global Security → Agents**
2. **TCP port for inbound agents:** Fixed → **50000**
3. Save configuration.

---

## Step 6: Open JNLP Port in Firewall on Jenkins Controller

```bash
sudo ufw allow 50000/tcp
sudo ufw allow 8080/tcp  # Web UI
sudo ufw reload
```

---

## Step 7: Test Docker Agent Connection

1. Go to **Manage Jenkins → Nodes → New Node**
2. Name it `docker-agent`, Label `docker-agent`
3. Use template or manually launch a container:

```bash
docker run -d --name docker-agent \
  -e JENKINS_URL=http://192.168.0.100:8080 \
  -e JENKINS_AGENT_NAME=docker-agent \
  -e JENKINS_AGENT_WORKDIR=/home/jenkins \
  -e JENKINS_SECRET=<agent-secret> \
  jenkins/inbound-agent
```

* Replace `<agent-secret>` with the token from Jenkins node configuration.
* Logs inside container should show **Agent successfully connected**

---

## Step 8: Verify in Jenkins UI

* Node `docker-agent` shows **online**
* Run a test pipeline:

```groovy
pipeline {
    agent { label 'docker-agent' }
    stages {
        stage('Test Agent') {
            steps {
                sh 'whoami'
                sh 'hostname'
                sh 'java -version'
            }
        }
    }
}
```

* Should run successfully inside Docker container.

---

## Step 9: Optional Optimizations

1. Set **restart policy** for containers:

   ```bash
   --restart unless-stopped
   ```
2. Limit max concurrent containers in Docker Cloud template based on VM CPU/memory.
3. Use **Docker volumes** if jobs need persistent workspace.
4. Monitor logs: `docker logs -f docker-agent`

---

🎯 With this setup, your Docker agents will:

* Auto-provision from Jenkins Cloud
* Connect reliably using fixed JNLP port
* Run pipelines without getting stuck
* Be fully manageable from Jenkins UI

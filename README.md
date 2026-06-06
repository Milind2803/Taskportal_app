# Task Management Portal
**Developer:** Milind Nagne  
**Stack:** Java 17 · Spring Boot 3 · MySQL · Docker · Kubernetes · Jenkins · SonarQube · Datadog

---

## Project Structure

```
taskportal/
├── src/
│   ├── main/java/com/milind/taskportal/
│   │   ├── TaskPortalApplication.java
│   │   ├── config/SecurityConfig.java
│   │   ├── controller/  (Auth, Dashboard, Task)
│   │   ├── model/       (User, Task)
│   │   ├── repository/  (UserRepository, TaskRepository)
│   │   ├── security/    (CustomUserDetailsService)
│   │   └── service/     (UserService, TaskService)
│   ├── main/resources/
│   │   ├── templates/   (login, register, dashboard, task form)
│   │   ├── application.properties          ← local H2
│   │   └── application-prod.properties     ← MySQL/production
│   └── test/
├── k8s/                  ← Kubernetes manifests
├── Dockerfile
├── docker-compose.yml    ← Full stack (App + MySQL + Jenkins + SonarQube)
├── Jenkinsfile           ← CI/CD pipeline
├── sonar-project.properties
└── pom.xml
```

---

## OPTION A — Run Locally (Fastest, No Docker Needed)

### Requirements
| Tool    | Version | Link |
|---------|---------|------|
| Java JDK | 17+   | https://adoptium.net |
| Maven   | 3.9+   | https://maven.apache.org |

### Steps
```bash
# 1. Enter project folder
cd taskportal

# 2. Build
mvn clean package -DskipTests

# 3. Run (auto uses H2 in-memory DB)
java -jar target/taskportal-1.0.0.jar

# 4. Open browser
http://localhost:8080/auth/register
```
> First registered user automatically becomes **ADMIN**.  
> H2 console at: http://localhost:8080/h2-console (URL: `jdbc:h2:mem:taskdb`, user: `sa`, pass: blank)

---

## OPTION B — Run with Docker Compose (Full Stack)

### Requirements
| Tool          | Version | Link |
|---------------|---------|------|
| Docker        | 24+     | https://docs.docker.com/get-docker |
| Docker Compose| v2+     | Included with Docker Desktop |

### Steps
```bash
cd taskportal

# Start everything (App + MySQL + Jenkins + SonarQube)
docker compose up -d --build

# Check status
docker compose ps

# Watch app logs
docker compose logs -f app
```

### Service URLs
| Service    | URL                        | Credentials |
|------------|----------------------------|-------------|
| App        | http://localhost:8080      | Register first |
| Jenkins    | http://localhost:8081      | See setup below |
| SonarQube  | http://localhost:9000      | admin / admin |

> **Note:** SonarQube takes ~2 minutes to start. Jenkins takes ~1 minute.

### Stop everything
```bash
docker compose down          # stop, keep data
docker compose down -v       # stop + delete volumes (clean slate)
```

---

## OPTION C — Jenkins CI/CD Pipeline

### Step 1 — Get Jenkins Initial Password
```bash
docker exec taskportal-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```
Open http://localhost:8081, paste the password, install suggested plugins.

### Step 2 — Install Required Plugins in Jenkins
Go to **Manage Jenkins → Plugins → Available** and install:
- `Pipeline`
- `Git`
- `Docker Pipeline`
- `SonarQube Scanner`
- `JaCoCo`
- `Kubernetes CLI`

### Step 3 — Configure SonarQube in Jenkins
1. Go to **Manage Jenkins → System**
2. Under **SonarQube servers** → Add:
   - Name: `SonarQube`
   - URL: `http://sonarqube:9000`
   - Token: Generate in SonarQube (Admin → My Account → Security → Generate Token)

### Step 4 — Configure Maven & JDK
Go to **Manage Jenkins → Tools**:
- JDK: Name = `JDK-17`, Install automatically, version = 17
- Maven: Name = `Maven-3.9`, Install automatically, version = 3.9.6

### Step 5 — Add Credentials
Go to **Manage Jenkins → Credentials → Global → Add Credential**:

| ID | Type | Value |
|----|------|-------|
| `dockerhub-creds` | Username/Password | Your Docker Hub login |
| `kubeconfig` | Secret file | Your `~/.kube/config` file |

### Step 6 — Create Pipeline Job
1. New Item → Pipeline → Name: `taskportal-pipeline`
2. Pipeline Definition: `Pipeline script from SCM`
3. SCM: Git, URL: `https://github.com/yourusername/taskportal`
4. Script Path: `Jenkinsfile`
5. Save → **Build Now**

### Pipeline Stages
```
Checkout → Build → Unit Tests → Code Coverage → SonarQube → Quality Gate → Docker Build → Docker Push → Deploy to K8s
```

---

## OPTION D — Kubernetes Deployment

### Requirements
| Tool      | Version | Install |
|-----------|---------|---------|
| kubectl   | 1.28+   | https://kubernetes.io/docs/tasks/tools |
| Minikube  | 1.32+   | https://minikube.sigs.k8s.io (for local K8s) |

### Step 1 — Start Minikube (local Kubernetes)
```bash
minikube start --memory=4096 --cpus=2
```

### Step 2 — Update your Docker image name
Edit `k8s/06-app-deployment.yaml` line with:
```yaml
image: yourdockerhubuser/taskportal:latest
```
Replace `yourdockerhubuser` with your actual Docker Hub username.

### Step 3 — Deploy to Kubernetes
```bash
# Apply all manifests
kubectl apply -f k8s/

# Verify pods are running
kubectl get pods -n taskportal

# Watch until all pods are Ready
kubectl get pods -n taskportal -w
```

### Step 4 — Access the App
```bash
# Get the service URL (Minikube)
minikube service taskportal-service -n taskportal --url

# Or port-forward
kubectl port-forward service/taskportal-service 8080:80 -n taskportal
```
Open: http://localhost:8080

### Useful kubectl Commands
```bash
# View all resources
kubectl get all -n taskportal

# Check logs
kubectl logs -l app=taskportal -n taskportal --tail=50

# Check HPA scaling
kubectl get hpa -n taskportal

# Delete everything
kubectl delete namespace taskportal
```

---

## OPTION E — SonarQube Code Analysis (Standalone)

### With SonarQube running via Docker Compose:

```bash
# 1. Start only SonarQube
docker compose up -d sonarqube sonar-db

# 2. Wait ~2 min, then open http://localhost:9000
#    Login: admin / admin  (change password on first login)

# 3. Create project manually:
#    Projects → Create Project → Local → Name: taskportal → token: copy it

# 4. Run analysis
mvn clean verify sonar:sonar \
  -Dsonar.projectKey=taskportal \
  -Dsonar.host.url=http://localhost:9000 \
  -Dsonar.token=YOUR_TOKEN_HERE

# 5. Open http://localhost:9000/dashboard?id=taskportal
```

---

## OPTION F — Datadog Monitoring

### With Docker Compose (add Datadog Agent):

```bash
# Set your Datadog API key
export DD_API_KEY=your_actual_api_key_here
export DD_APP_KEY=your_actual_app_key_here

# Start the app with Datadog enabled
docker compose up -d
```

### What gets monitored:
- **App metrics** — HTTP request rate, latency, error rate (via Micrometer)
- **Health endpoint** — http://localhost:8080/actuator/health
- **JVM metrics** — heap, GC, threads
- **Custom metrics** — task counts per status

### Metrics Endpoints
| Endpoint | What it shows |
|----------|--------------|
| /actuator/health | App health status |
| /actuator/metrics | All available metrics |
| /actuator/info | App info (name, version, developer) |

### Datadog Dashboard Setup
1. Log in to https://app.datadoghq.com
2. Go to **Dashboards → New Dashboard**
3. Add widgets for:
   - `jvm.memory.used` — JVM heap usage
   - `http.server.requests` — request throughput
   - `system.cpu.usage` — CPU load

---

## Environment Variables Reference

| Variable | Example | Used In |
|----------|---------|---------|
| `DB_URL` | `jdbc:mysql://mysql-service:3306/taskportal?...` | App (prod) |
| `DB_USERNAME` | `appuser` | App (prod) |
| `DB_PASSWORD` | `apppass` | App (prod) |
| `DD_API_KEY` | `abc123...` | Datadog metrics |
| `DD_APP_KEY` | `xyz789...` | Datadog metrics |
| `SPRING_PROFILES_ACTIVE` | `prod` | Switches to MySQL |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| Port 8080 already in use | `java -jar app.jar --server.port=9090` |
| Docker compose app fails | Wait for MySQL health check: `docker compose logs mysql` |
| SonarQube not starting | Increase Docker memory to 4GB in Docker Desktop settings |
| K8s pod CrashLoopBackOff | `kubectl describe pod <pod-name> -n taskportal` |
| Build fails | Check Java version: `java -version` (must be 17+) |
| Maven not found | Add Maven to PATH or use `./mvnw` wrapper |

---

*Task Management Portal — Milind Nagne*

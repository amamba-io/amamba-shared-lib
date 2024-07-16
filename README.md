# amamba-shared-lib

# Usage
this is a shared library for jenkins pipeline, use docker to run custom steps.

use amamba custom shared library in jenkinsfile:
```
amambaCustomStep(
              pluginID: 'deploy_application', 
              version: 'v1.0.0',
              docker: [
                  image: 'docker.m.daocloud.io/amambadev/jenkins-agent-base:v0.3.2-podman',
                  script: 'env && kubectl apply -f ${filepath} -n ${namespace}'
              ],
              args: [
                  cluster: 'zcl-test',
                  namespace: 'default',
                  filepath: 'dao-2048'
                  ])
            }
```

withCredential:

```
withCredentials([file(credentialsId:'kubeconfig',variable:'KUBECONFIG')]) {
            amambaCustomStep(
              pluginID: 'deploy_application', 
              version: 'v1.0.0',
              docker: [
                  image: 'docker.m.daocloud.io/amambadev/jenkins-agent-base:v0.3.2-podman',
                  script: 'env && kubectl apply -f ${filepath} -n ${namespace}'
              ],
              args: [
                  cluster: 'zcl-test',
                  namespace: 'default',
                  filepath: 'dao-2048'
                  ])
            }
          }
```

import cn.k8ops.jenkins.Git
import com.cloudbees.groovy.cps.NonCPS
import groovy.transform.Field
import org.yaml.snakeyaml.Yaml

/*
library 'apl@develop'

env.PIPELINE_CONFIG = '''
git:
  sshUrl: git@github.com:liuyongsheng/test.git
  branch: refs/heads/master

environment:
  APP_GROUP: sample
  APP_NAME: sample
  APP_ENV: test
  APP_VERSION: '1.0.0'
  APP_BRANCH: master
  APP_HOSTS: 127.0.0.1

pipeline:
- name: build:
  steps:
  - script
      code: |
        echo "hello, ${APP_VERSION}"

workflow:
- name: build
  stage: build
'''

aplRunner(env.PIPELINE_CONFIG)
 */

/** plugins:
 * 1. pipeline-utility-steps
 * 2. rebuild-plugins
 */

@Field gitCredentialsId = "gitlab-cibuild"

def call(String yamlConfig) {

    env.PIPELINE_CONFIG = "" // 清理多行文本环境变量

    node {
          try {
              // dep: pipeline-utility-steps
              def yaml = readYaml text: (yamlConfig?:'')
              build(yaml)
              echo "--//INFO: Finish Build."
          } catch (err) {
              echo "--//ERR: Build(with error)"
              updateBuildStatus('FAILURE')
              throw err
          }
    }
}

def build(def yaml) {

    // config PIPELINE_CONFIG
    pipeline = yaml.pipeline

    if (pipeline == null) {
        echo "--//ERR: pipeline is not set."
        updateBuildStatus("FAILURE")
        return
    }

    // config PIPELINE_CONFIG
    workflow = yaml.workflow
    if (workflow == null || workflow.size() == 0) {
        echo "--//ERR: workflow is not set."
        updateBuildStatus("FAILURE")
        return
    }


    // checkout code
    if (yaml.git?.sshUrl) {
        gitConfig = yaml.git
        sshCredentialsId = gitCredentialsId
        if (gitConfig.sshCredentialsId) {
            sshCredentialsId = gitConfig.sshCredentialsId
        }
        cleanWs()
        new Git().checkoutGitRepository(".", gitConfig.sshUrl, gitConfig.branch, sshCredentialsId)
    }


    workflow.each {

        def stageName = ""
        def stages = ""
        if (it instanceof Map) {
            stageName = it.name
            stages = it.stages
        } else {
            stageName = it
            stages = it
        }

        stage("${stageName}") {
            aslPipeline(
                    from: "jenkins",
                    content: dumpAsMap(yaml),
                    properties: "run.stages=${stages}",
            )
        }
    }
}

def updateBuildStatus(String status) {
    currentBuild.result = status
}

@NonCPS
def dumpAsMap(config) {
    if (config instanceof String) {
        return config
    } else {
        def yaml = new Yaml()
        return yaml.dumpAsMap(config)
    }
}

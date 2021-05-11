package cn.k8ops.jenkins


/**
 *
 * Git functions
 *
 */

/**
 * Checkout single git repository
 *
 * @param path            Directory to checkout repository to
 * @param url             Source Git repository URL
 * @param branch          Source Git repository branch
 * @param credentialsId   Credentials ID to use for source Git
 */
def checkoutGitRepository(path, url, branch, credentialsId = null){

    dir(path) {
        def gitParams = [
                $class                           : 'GitSCM',
                branches                         : [[name: "${branch}"]],
                doGenerateSubmoduleConfigurations: false,
                extensions                       : [[$class: 'CleanBeforeCheckout']],
                submoduleCfg                     : [],
                userRemoteConfigs                : [[url: "${url}"]]
        ]

        if (credentialsId != null) {
            gitParams.userRemoteConfigs = [[credentialsId: "${credentialsId}",
                                            url          : "${url}"]]
        }

        if (env.gitlabActionType != null && env.gitlabActionType == 'MERGE') {

            sh "git config --global user.email \"${env.gitlabUserEmail}\""
            sh "git config --global user.name \"${env.gitlabUserName}\""

            gitParams.branches = [[name: "origin/${env.gitlabSourceBranch}"]]
            gitParams.extensions = [
                    [
                            $class : 'PreBuildMerge',
                            options: [
                                    fastForwardMode: 'FF',
                                    mergeRemote    : 'origin',
                                    mergeStrategy  : 'default',
                                    mergeTarget    : "${env.gitlabTargetBranch}"
                            ]
                    ],
                    [
                            $class: 'CleanBeforeCheckout'
                    ]
            ]

        }

        checkout(gitParams)
    }
}

String getGitCommit() {
    sh(
            returnStdout: true, script: 'git rev-parse HEAD',
            label : 'getting GIT commit'
    ).trim()
}

String getGitCommitAuthor() {
    sh(
            returnStdout: true, script: "git --no-pager show -s --format='%an (%ae)' HEAD",
            label : 'getting GIT commit author'
    ).trim()
}

String gitGitCommitMessage() {
    sh(
            returnStdout: true, script: "git log -1 --pretty=%B HEAD",
            label : 'getting GIT commit message'
    ).trim()
}

String getGitCommitTime() {
    sh(
            returnStdout: true, script: "git show -s --format=%ci HEAD",
            label : 'getting GIT commit date/time'
    ).trim()
}

String getGitBranch() {
    // in case code is already checked out, OpenShift build config can not be used for retrieving branch
    def branch = sh(
            returnStdout: true,
            script: "git rev-parse --abbrev-ref HEAD",
            label : 'getting GIT branch to build').trim()

    branch = sh(
            returnStdout: true,
            script: "git name-rev ${branch} | cut -d ' ' -f2  | sed -e 's|remotes/origin/||g'",
            label : 'resolving to real GIT branch to build').trim()

    return branch
}

// looks for string [ci skip] in commit message
boolean getCiSkip() {
    sh(returnStdout: true, script: 'git show --pretty=%s%b -s',
            label : 'check skip CI?'
    ).toLowerCase().contains('[ci skip]')
}

return this
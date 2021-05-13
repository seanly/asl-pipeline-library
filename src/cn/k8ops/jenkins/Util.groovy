package cn.k8ops.jenkins

def genSecretList(secrets) {

    def ret = []

    for (int i=0; i < secrets.size(); i++) {
        def s = secrets[i]

        if (s.type == "usernamePassword") {
            ret << usernamePassword(credentialsId: s.source,
                    usernameVariable: s.target.usernameVariable,
                    passwordVariable: s.target.passwordVariable)
        } else if (s.type == "sshUserPrivateKey") {
            ret << sshUserPrivateKey(credentialsId: s.source,
                    keyFileVariable: s.target.keyFileVariable,
                    passphraseVariable: s.target.passphraseVariable,
                    usernameVariable: s.target.usernameVariable)
        } else if (s.type == "FileBinding") {
            ret << [
                    $class: 'FileBinding',
                    credentialsId: s.source,
                    variable: s.target.variable
            ]
        } else {
            error "${s.type} is not support"
        }
    }

    return ret
}

return this

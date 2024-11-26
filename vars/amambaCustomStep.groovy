def call(Map config) {
/*
config format:
    pluginID: "pluginID"
    version: "version"
    args: [
        key1: "value1",
        key2: "value2"
    ]
    docker: {
        image: "image",
        script: "script",
        shell: "shell",
        entrypoint: "entrypoint"
    }
*/

    validate(config)

    runArgs = genRunArgs(config)
    path = createOutputDirAndReturnPath()
    runArgs = runArgs + " -v "+ path +":/tmp:rw "
    script {
        def dockerExists = sh(script: 'which docker', returnStatus: true) == 0
        if (!dockerExists) {
            error "Unable to find command 'docker', please update running image by `withContainer`, such as withContainer('base')"
            return
        }

        if (env.PLUGIN_REGISTRY_USER && env.PLUGIN_REGISTRY_PASSWORD) {
            def registry = getRegistryFromImage(config.docker.image)
            sh "docker login -u ${env.PLUGIN_REGISTRY_USER} -p ${env.PLUGIN_REGISTRY_PASSWORD} ${registry}"
        }

        docker.image(config.docker.image).inside("$runArgs") {
            def scriptName = "script_${System.currentTimeMillis()}_${UUID.randomUUID().toString()}"
            writeFile file: scriptName, text: config.docker.script
            sh "chmod +x ${scriptName}"
            if (config.docker.shell) {
                sh "${config.docker.shell} ${scriptName}"
            } else if (config.docker.script.trim().startsWith("#!")) {
                sh "./${scriptName}"
            } else {
                sh "${config.docker.script}"
            }
        }
    }
    // all returned data should be placed in this file.
    output = sh(returnStdout: true, script: "cat ${path}/output")
    return output
}

def createOutputDirAndReturnPath() {
     def randomString = UUID.randomUUID().toString().replaceAll("-", "")[0..7]
     path = "/tmp/" + randomString
     sh "mkdir ${path} && touch ${path}/output"
     return path
}

def genRunArgs(Map config) {
    def workspace = pwd()
    runArgs = "--privileged --network=host "

    if (config.docker.entrypoint) {
        runArgs = runArgs + "--entrypoint=${config.docker.entrypoint} "
    }

    //  write args to file as envFile
    def randomString = UUID.randomUUID().toString().replaceAll("-", "")[0..7]
    filePath = randomString + "-" +  "amamba_env.txt"

    envContent = flattenArgs(config.args)
    writeFile file: "${filePath}", text: envContent

    runArgs = runArgs + " --env-file ${workspace}/${filePath}"
    return runArgs
}

def flattenArgs(Map args) {
    flattenArgsInternal(args, "")
}

def flattenArgsInternal(Map args, String prefix) {
    args.collect { key, value ->
        if (value instanceof Map) {
            flattenArgsInternal(value, "${prefix}${prefix.empty ? '' : '_'}$key")
        } else {
            ["${prefix}${prefix.empty ? '' : '_'}$key=$value"]
        }
    }.flatten().join("\n")
}


def validate(Map config) {
    if (!config.pluginID) {
        error "pluginID is required"
    }
    if (!config.version) {
        error "version is required"
    }

    if (!config.args) {
        error "args is required"
    }

    if (!config.docker) {
        error "docker config is required"
    }

    if (!config.docker.image) {
        error "docker.image is required"
    }
    if (!config.docker.script) {
        error "docker.script is required"
    }
    return this
}

def getRegistryFromImage(imageName) {
    def matcher = imageName =~ /^([^\/]+)\/.*/
    if (matcher) {
        def registry = matcher[0][1]
        if (registry.contains('.')) {
            return registry
        }
    }
    return null
}

return this

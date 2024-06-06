def call(Map config) {
    validate(config)

    Map args = config.args
    runArgs = genRunArgs(args)

    script {
        def dockerExists = sh(script: 'which docker', returnStatus: true) == 0
        if (!dockerExists) {
            error "Unable to find command 'docker', please update running image by `withContainer`, such as withContainer('base')"
            return
        }
        docker.image(args.image).inside("$runArgs") {
            if (args.shell) {
                sh "${args.shell} ${args.script}"
            } else {
                sh "${args.script}"
            }
        }
    }
}

def genRunArgs(Map args) {
    def workspace = pwd()
    runArgs = ""

    if (args.entrypoint) {
        runArgs = "--entrypoint=${args.entrypoint} "
    }

    //  write args to file as envFile
    filePath = "amamba_env.txt"

    envContent = flattenArgs(args)
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
    if (!config.args.image) {
        error "args.image is required"
    }
    if (!config.args.script) {
        error "args.script is required"
    }
    return this
}

return this

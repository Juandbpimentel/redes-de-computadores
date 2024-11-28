class_name UPNPJava extends Node

# add port mapping
# java -jar portmapper-2.2.3.jar -add -externalPort 10001 -internalPort 10001  -protocol udp -lib org.chris.portmapper.router.weupnp.WeUPnPRouterFactory
# remove port mapping
# java -jar portmapper-2.2.3.jar -delete -externalPort 10001 -protocol udp -lib org.chris.portmapper.router.weupnp.WeUPnPRouterFactory

static func addPortMapping(externalPort:int, internalPort:int, protocol:String) -> Error:
    if OS.get_name() == "Linux":
        var change_dir_arguments = "cd {jarPackagePath}"
        var jarPackagePath = ProjectSettings.globalize_path("res://utilities")
        var execute_arguments = "java -jar portmapper-2.2.3.jar -add -externalPort {externalPort} -internalPort {internalPort} -protocol {protocol} -lib org.chris.portmapper.router.weupnp.WeUPnPRouterFactory -description \"Godot Game port\""
        var executeString = str(change_dir_arguments," && ",execute_arguments).format({"jarPackagePath": jarPackagePath, "externalPort":externalPort, "internalPort":internalPort, "protocol":protocol})
        
        var result = OS.execute("bash",["-c",executeString], [],true,false)
        if result == -1:
            print("Error: ", result)
            return FAILED
        return OK
    elif OS.get_name() == "Windows":
        var change_dir_arguments = "cd {jarPackagePath}"
        var jarPackagePath = ProjectSettings.globalize_path("res://utilities")
        var execute_arguments = "java -jar portmapper-2.2.3.jar -add -externalPort {externalPort} -internalPort {internalPort} -protocol {protocol} -lib org.chris.portmapper.router.weupnp.WeUPnPRouterFactory -description \"Godot Game port\""
        var executeString = str("/C ",change_dir_arguments.replace("/","\\")," && ",execute_arguments).format({"jarPackagePath": jarPackagePath, "externalPort":externalPort, "internalPort":internalPort, "protocol":protocol})
        
        var result = OS.execute("cmd.exe",[executeString], [],true,false)
        if result == -1:
            print("Error: ", result)
            return FAILED
        return OK
    else:
        print("OS not supported")
        return FAILED

static func deletePortMapping(externalPort:int, internalPort:int, protocol:String) -> Error:
    if OS.get_name() == "Linux":
        var change_dir_arguments = "cd {jarPackagePath}"
        var jarPackagePath = ProjectSettings.globalize_path("res://utilities")
        var execute_arguments = "java -jar portmapper-2.2.3.jar -delete -externalPort {externalPort} -internalPort {internalPort} -protocol {protocol} -lib org.chris.portmapper.router.weupnp.WeUPnPRouterFactory"
        var executeString = str(change_dir_arguments," && ",execute_arguments).format({"jarPackagePath": jarPackagePath, "externalPort":externalPort, "internalPort":internalPort, "protocol":protocol})
        
        var result = OS.execute("bash",["-c",executeString], [],true,false)
        if result == -1:
            print("Error: ", result)
            return FAILED
        return OK
    elif OS.get_name() == "Windows":
        var change_dir_arguments = "cd {jarPackagePath}"
        var jarPackagePath = ProjectSettings.globalize_path("res://utilities")
        var execute_arguments = "java -jar portmapper-2.2.3.jar -delete -externalPort {externalPort} -internalPort {internalPort} -protocol {protocol} -lib org.chris.portmapper.router.weupnp.WeUPnPRouterFactory"
        var executeString = str("/C ",change_dir_arguments.replace("/","\\")," && ",execute_arguments).format({"jarPackagePath": jarPackagePath, "externalPort":externalPort, "internalPort":internalPort, "protocol":protocol})
        
        var result = OS.execute("cmd.exe",[executeString], [],true,false)
        if result == -1:
            print("Error: ", result)
            return FAILED
        return OK
    else:
        print("OS not supported")
        return FAILED
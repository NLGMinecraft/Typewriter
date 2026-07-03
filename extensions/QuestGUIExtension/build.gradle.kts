repositories {}

dependencies {
    compileOnly(project(":QuestExtension"))
}

typewriter {
    namespace = "questgui"

    extension {
        name = "QuestGUI"
        shortDescription = "A quest gui extension"
        description = "A long description of the extension that gives players a gui for quests. It is super nice and super big."
        engineVersion = rootProject.extra["typewriterEngineVersion"] as String
        channel = com.typewritermc.moduleplugin.ReleaseChannel.NONE
        dependencies {
            dependency("typewritermc", "Quest")
        }
        paper()
    }
}
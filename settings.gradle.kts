pluginManagement {
    repositories {
        // 阿里云镜像（解决下载超时）
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/maven-central")
        maven("https://maven.aliyun.com/repository/gradle-plugin")

        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/maven-central")

        google()
        mavenCentral()
    }
}

rootProject.name = "MusicPlayer"
include(":app")
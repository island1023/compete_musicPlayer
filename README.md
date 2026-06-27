# NeteaseCloudMusic Player

基于网易云 API 接口实现的 Android 音乐播放器

## 功能预览

### 主界面

| 首页 | 发现 | 推荐 |
|:---:|:---:|:---:|
| ![首页](./screenshots/首页.png) | ![发现页面](./screenshots/发现页面.png) | ![推荐](./screenshots/推荐.png) |

| 发现页华语歌单 | 歌手榜 | 主播新人榜 |
|:---:|:---:|:---:|
| ![发现页华语歌单](./screenshots/发现页华语歌单.png) | ![歌手榜](./screenshots/歌手榜.png) | ![主播新人榜](./screenshots/主播新人榜.png) |

| MV | MV 可播放 | 电台 |
|:---:|:---:|:---:|
| ![MV](./screenshots/MV.png) | ![MV可播放](./screenshots/MV可播放.png) | ![电台](./screenshots/电台.png) |

### 登录

| 登录界面 | 二维码登录 |
|:---:|:---:|
| ![登录界面](./screenshots/登录界面.png) | ![二维码登录](./screenshots/二维码登录.png) |

登录采用二维码登录，同时支持密码和验证码登录。由于在模拟器运行，没有设备码，无法正确登录，会显示账号有风险。设置了登录持久化，避免频繁登录造成账号封禁。

### 播放 & 歌单

| 当前歌单播放队列 | 歌单收藏 | 收藏的歌单 |
|:---:|:---:|:---:|
| ![当前歌单播放队列](./screenshots/当前歌单播放队列.png) | ![歌单收藏](./screenshots/歌单收藏.png) | ![收藏的歌单](./screenshots/收藏的歌单.png) |

| 创建本地歌单 | 成功创建本地歌单 | 加号加入歌单 |
|:---:|:---:|:---:|
| ![创建本地歌单](./screenshots/创建本地歌单.png) | ![成功创建本地歌单](./screenshots/成功创建本地歌单.png) | ![加号加入歌单](./screenshots/加号加入歌单.png) |

### 歌曲操作

| 歌曲评论 | 喜欢的歌曲 | 我喜欢的音乐 |
|:---:|:---:|:---:|
| ![歌曲评论](./screenshots/歌曲评论.png) | ![喜欢的歌曲](./screenshots/喜欢的歌曲.png) | ![我喜欢的音乐](./screenshots/我喜欢的音乐.png) |

| 可以正常下载 | 上传内容 | 搜索 |
|:---:|:---:|:---:|
| ![可以正常下载](./screenshots/可以正常下载.png) | ![上传内容](./screenshots/上传内容.png) | ![搜索](./screenshots/搜索.png) |

### 个人中心

| 我的部分 | 最近播放 | 本地音乐 |
|:---:|:---:|:---:|
| ![我的部分](./screenshots/我的部分.png) | ![最近播放](./screenshots/最近播放.png) | ![本地音乐](./screenshots/本地音乐.png) |

| 更换头像 | 头像已更新 | 关注粉丝 |
|:---:|:---:|:---:|
| ![更换头像](./screenshots/更换头像.png) | ![头像已更新](./screenshots/头像已更新.png) | ![关注粉丝](./screenshots/关注粉丝.png) |

| 设置页面 | 笔记 |
|:---:|:---:|
| ![设置页面](./screenshots/设置页面.png) | ![笔记](./screenshots/笔记.png) |

## 技术栈

- **前端**: Android (Fragment 架构)
- **后端**: [NeteaseCloudMusicApi (api-enhanced-main)](https://github.com/your-repo/api-enhanced-main)

## 后端部署

使用 GitHub 开源项目 `NeteaseCloudMusicApi` 获取 API 接口：

```bash
# 1. 进入项目目录
cd E:\APP\music\springapi\NeteaseCloudMusicApi\api-enhanced-main\api-enhanced-main

# 2. 安装依赖（第一次运行必须执行）
npm install

# 3. 启动服务
node app.js
```

## 项目结构

- `首页` - 推荐、音乐、电台
- `发现` - 各种类别，华语、摇滚等
- `视频` - MV
- `我的` - 个人中心、本地歌单、收藏

- 搜索、设置

# TurtleTV
一个用Node.js实现的简单的实时直播平台，目前仅支持RTMP协议，代码工作才刚刚开始。
A simple live video streaming platfrom implemented with Node.js, supports RTMP protocol only at present. The coding was just started not for long.

# References & Thanks
本项目只是想整合出一个demo，对下面的大神们的工作表示强烈推荐和衷心感谢！
This project is only in the purpose of making a demo, extremly recommendation and thanks to the good people's work below:
- Node Media Server.
  https://github.com/illuspas/Node-Media-Server
- node-rtmpapi
  https://github.com/delian/node-rtmpapi
- PLDroidPlayer
  https://github.com/pili-engineering/PLDroidPlayer

# Usage
- 通过/server/db.sql建立MySQL数据库。Create a MySQL database with /server/db.sql.
- 修改数据库配置/server/mysql.json。Modify the database config in /server/mysql.json.
- 运行服务器，命令行：node app.js。Run the server with command 'node app.js'.
- 安装运行/client-android/TurtleTV.apk，并在设置界面中修改服务器IP。Install and run the /client-android/TurtleTV.apk and modify the server IP in main menu>Settings。
- 注册并申请开通直播。Register and Apply to become a publisher.
- 根据“我的直播”中的信息修改OBS的广播设定。modify OBS's broadcast settings according to the info in 'My Publishing'.
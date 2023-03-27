# savetime

some daily bedding code for jupyter kotlin more detail
see [user guide](https://github.com/zhangliangbo/notebook/blob/main/savetime.ipynb)
or [user guide](http://nbviewer.org/github/zhangliangbo/notebook/blob/main/savetime.ipynb).

# 基本设计

核心对象的核心流程。

所有连接信息通过配置文件指定，适用多环境，每个环境的连接信息通过`key`来指定。

# 门面

```
io.github.zhangliangbo.savetime.ST
```

充分利用笔记本的提示功能。

# ssh

powered by `jsch`.

## 配置文件ssh.json

```json
{
  "local": {
    "host": "xxx.xxx.xxx.xxx",
    "port": 22,
    "username": "xxx",
    "password": "xxx"
  },
  "dev": {
    "host": "xxx.xxx.xxx.xxx",
    "port": 22,
    "username": "xxx",
    "password": "xxx"
  }
}
```

## 设置配置文件

```
ST.ssh.setConfig(File("${prefix}ssh.json").toURI().toURL())
```

# jdbc

powered by `commons-dbutils`

可配置ssh，支持自动重连

# 配置文件mysql.json

```json
{
  "local": {
    "db1": {
      "url": "jdbc:xxx:xxx",
      "username": "xxx",
      "password": "xxx"
    },
    "db2": {
      "url": "jdbc:xxx:xxx",
      "username": "xxx",
      "password": "xxx"
    }
  },
  "dev": {
    "db1": {
      "url": "jdbc:xxx:xxx",
      "username": "xxx",
      "password": "xxx",
      "ssh": "dev"
    },
    "db2": {
      "url": "jdbc:xxx:xxx",
      "username": "xxx",
      "password": "xxx",
      "ssh": "dev"
    }
  }
}
```

## 设置配置文件

```
ST.jdbc.setConfig(File("${prefix}mysql.json").toURI().toURL())
```

## 常用功能

### 查询进程
```
ST.jdbc.showProcessList("dev","db1")
```
### 查询全局变量
```
ST.jdbc.showGlobalVariable("dev","db1","thread")
```
### 查询建表语句
```
ST.jdbc.createTableSql("dev","db1","table")
```
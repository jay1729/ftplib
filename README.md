# ftplib

ftplib is a java library that you can use to make an ftp client

## Features

* Both Active and Passive mode supported
* Search files by regex to get a files in a tree structure
* Download files/folders by regex

## How to use

### Setup client

```java
    import com.jayanthag.ftp.FTPClient;

    ...
    ...
    ...

    FTPClient ftpClient = new FTPClient(host, port);
    ftpClient.connect();
    ftpClient.login(username, password);

    ...
    ...

    ftpClient.stopClient();
```

### Get all files/folders in current dir

```java
   import com.jayanthag.ftp.models.FileElement;

   ...
   ...
   ...

   ArrayList<FileElement> files = ftpClient.ls();
   /* a FileElement object represents a folder/file,
    it may hold references to files/folders inside itself,
    and also to its parent
    /*
```

### Download all python files

```java
    ...
    ...
    ...

    ftpClient.regexDownload(".py$");
```
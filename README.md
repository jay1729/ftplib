# ftplib

ftplib is a java library that you can use to make an ftp client

## How to use

### Setup client

```java
    import com.jayanthag.ftp.FTPClient;

    ...

    FTPClient ftpClient = new FTPClient(host, port);
    ftpClient.connect();
    ftpClient.login(username, password);
```
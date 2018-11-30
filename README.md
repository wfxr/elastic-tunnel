# elastic-tunnel

Tools for downloading data from elasticsearch server.

## Usage

```
usage: elastic-tunnel -f <arg> -h <arg> -i <arg> [-l <arg>] -o <arg> 
       [-p <arg>] [--pretty] [--scroll <arg>] [--size <arg>] [-u <arg>]
    -f, --fields <arg>   The source fields to download
    -h, --host <arg>     Elasticsearch host url
    -i, --index <arg>    Elasticsearch index name or alias
    -l, --limit <arg>    Max entries to download
    -o, --output <arg>   Output format [JSON|CSV]
    -p, --pass <arg>     Elasticsearch user password
        --pretty         Pretty printing
        --scroll <arg>   Elasticsearch scroll timeout
        --size <arg>     Elasticsearch scroll size
    -u, --user <arg>     Elasticsearch user name
```

You need to enter a query body or redirected it from file, eg:

```
java -jar elastic-tunnel.jar -h http://127.0.0.1:9200 -i twitter --fields=id,user -o csv < query.json
```

`query.json` may like this:

```bash
{
  "term": {
    "country": "China"
  }
}
```

## [License](LICENSE.txt)

The MIT License (MIT)

Copyright (c) 2018 Wenxuan Zhang

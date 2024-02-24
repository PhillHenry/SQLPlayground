# SQLPlayground
Katas for various SQL engines

# Running with MS SQL Server

Microsoft's SQL server can actually be run on Linux via Docker! Run it with:

`docker run -e "ACCEPT_EULA=Y" -e "MSSQL_SA_PASSWORD=St0ngPa55wd!" -p 1433:1433 --name sql1 --hostname sql1 -d mcr.microsoft.com/mssql/server:2022-latest`
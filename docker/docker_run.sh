docker run -d \
-p 8181:8181 \
--name ice-rest \
--net lakenet \
--ip 192.168.10.16 \
-e REST_CATALOG_NAME=rest \
-e REST_CATALOG_ACCESS_TOKEN_SECRET=fastrun-run \
-e CATALOG_IO__IMPL=org.apache.iceberg.aws.s3.S3FileIO \
-e CATALOG_WAREHOUSE=s3://lakehouse/iceberg/warehouse \
-e CATALOG_S3_ENDPOINT=http://192.168.10.12:9000 \
-e CATALOG_URI=jdbc:mysql://192.168.10.11:3306/iceberg \
-e CATALOG_JDBC_USER=root \
-e CATALOG_JDBC_PASSWORD=123456 \
-e AWS_ACCESS_KEY_ID=dupeng \
-e AWS_SECRET_ACCESS_KEY=dupeng01 \
-e AWS_REGION=zh-east-1 \
iceberg-rest:1.0
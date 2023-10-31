package run.fastrun.iceberg.catalog.rest;

import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.CatalogUtil;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.jdbc.JdbcCatalog;
import org.apache.iceberg.rest.RESTCatalogAdapter;
import org.apache.iceberg.rest.RESTCatalogServlet;
import org.apache.iceberg.util.PropertyUtil;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.fastrun.iceberg.catalog.rest.filter.RequestFilter;
import run.fastrun.iceberg.catalog.rest.utils.StringUtils;

import javax.servlet.DispatcherType;
import java.io.File;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * @Auther: dupeng
 * @Date: 2023/10/27/22:09
 * @Description:
 */
public class IcebergRestCatalogServer {
    private static final Logger LOG = LoggerFactory.getLogger(IcebergRestCatalogServer.class);
    private static final String CATALOG_ENV_PREFIX = "CATALOG_";

    private static final String ENV_REST_CATALOG_NAME = "REST_CATALOG_NAME";

    private IcebergRestCatalogServer() {}

    public static void main(String[] args) {
        try {
            RESTCatalogAdapter adapter = new RESTCatalogAdapter(backendCatalog());
            RESTCatalogServlet servlet = new RESTCatalogServlet(adapter);
            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
            context.setContextPath("/");
            ServletHolder servletHolder = new ServletHolder(servlet);
            servletHolder.setInitParameter("javax.ws.rs.Application", "ServiceListPublic");
            context.addFilter(RequestFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
            context.addServlet(servletHolder, "/*");
            context.setVirtualHosts(null);
            context.setGzipHandler(new GzipHandler());

            Server httpServer =
                    new Server(PropertyUtil.propertyAsInt(System.getenv(), "REST_PORT", 8181));
            httpServer.setHandler(context);

            httpServer.start();
            LOG.info("REST Catalog Server started...");

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }



    private static Catalog backendCatalog() throws IOException, ClassNotFoundException {
        // Translate environment variable to catalog properties
        Map<String, String> catalogProperties =
                System.getenv().entrySet().stream()
                        .filter(e -> e.getKey().startsWith(CATALOG_ENV_PREFIX))
                        .collect(
                                Collectors.toMap(
                                        e ->
                                                e.getKey()
                                                        .replaceFirst(CATALOG_ENV_PREFIX, "")
                                                        .replaceAll("__", "-")
                                                        .replaceAll("_", ".")
                                                        .toLowerCase(Locale.ROOT),
                                        Map.Entry::getValue,
                                        (m1, m2) -> {
                                            throw new IllegalArgumentException("Duplicate key: " + m1);
                                            },
                                        HashMap::new));

        // Fallback to a JDBCCatalog impl if one is not set
        catalogProperties.putIfAbsent(
                CatalogProperties.CATALOG_IMPL, "org.apache.iceberg.jdbc.JdbcCatalog");


        switch (catalogProperties.get(CatalogProperties.URI).split(":")[1]){
            case "mysql":
                Class.forName("com.mysql.cj.jdbc.Driver");
                LOG.info("JDBC Driver: com.mysql.cj.jdbc.Driver");
                break;
            case "postgresql":
                Class.forName("org.postgresql.Driver");
                LOG.info("JDBC Driver: org.postgresql.Driver");
                break;
            default:
                Class.forName("org.sqlite.JDBC");
                LOG.info("JDBC Driver: org.sqlite.JDBC");
        }

        catalogProperties.putIfAbsent(
                CatalogProperties.URI, "jdbc:sqlite:file:/tmp/iceberg_rest_mode=memory");

        catalogProperties.putIfAbsent(
                JdbcCatalog.PROPERTY_PREFIX + "user", "user");

        catalogProperties.putIfAbsent(
                JdbcCatalog.PROPERTY_PREFIX + "password", "password");


        // Configure a default location if one is not specified
        String warehouseLocation = catalogProperties.get(CatalogProperties.WAREHOUSE_LOCATION);
        if (warehouseLocation == null) {
            File tmp = java.nio.file.Files.createTempDirectory("iceberg_warehouse").toFile();
            tmp.deleteOnExit();
            warehouseLocation = tmp.toPath().resolve("iceberg_data").toFile().getAbsolutePath();
            catalogProperties.put(CatalogProperties.WAREHOUSE_LOCATION, warehouseLocation);
            LOG.info("No warehouse location set.  Defaulting to temp location: {}", warehouseLocation);
        }

        LOG.info("Catalog with properties: {}", catalogProperties);
        String catalogName = StringUtils.getEnv(ENV_REST_CATALOG_NAME);
        LOG.info("REST Catalog Name: {}", catalogName);
        return CatalogUtil.buildIcebergCatalog(catalogName, catalogProperties, new Configuration());
    }


}

package org.eugene.controller;

import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.hadoop.fs.Path;
import org.eugene.core.common.AWSS3Reader;
import org.eugene.core.common.AzureStorageReader;
import org.eugene.model.CommonData;
import org.eugene.model.TableMeta;
import org.eugene.persistent.VirtualDB;
import org.eugene.ui.Constants;
import org.eugene.ui.Dashboard;
import org.eugene.ui.Main;
import org.eugene.ui.Table;

import java.io.File;
import java.util.List;
import java.util.Map;

public class Renderer {

    private Stage stage;
    private TableRenderer tableRenderer;
    private DashboardRenderer dashboardRenderer;

    private List<String> showingList;

    public Renderer(Stage stage){
        this.stage = stage;
        tableRenderer = new TableRenderer();
        dashboardRenderer = new DashboardRenderer();
    }

    public void initUI(){
        Table table = new Table(stage);
        Dashboard dashboard = new Dashboard(stage);
        tableRenderer.setTable(table);
        dashboardRenderer.setDashboard(dashboard);
        Main main = new Main(stage, table, dashboard);
        main.initUI();
    }

    private boolean load(Path path){
        DataParser dataParser;
        if (path.toString().toLowerCase().endsWith("orc")){
            dataParser = new ORCDataParser();
        }else if(path.toString().toLowerCase().endsWith("avro")){
            dataParser = new AVRODataParser();
        }else{
            dataParser = new ParquetDataParser();
        }
        boolean status = dataParser.parseData(path);
        if (status) {
            tableRenderer.init();
            CommonData commonData = VirtualDB.getInstance().getCommonData();
            TableMeta tableMeta = VirtualDB.getInstance().getTableMeta();
            showingList = commonData.getPropertyList();
            dashboardRenderer.refreshMetaInfo(commonData.getSchema(), path.toString(), tableMeta.getRow(), tableMeta.getColumn());
            tableRenderer.refresh(showingList, commonData.getPropertyList(), tableMeta.getRow(), tableMeta.getColumn(), commonData.getData());
        }
        return status;
    }

    public boolean loadAndShow(Map<String, String> map){
        AWSS3Reader awss3Reader = new AWSS3Reader();
        Path path = awss3Reader.read(map.get(Constants.BUCKET), map.get(Constants.FILE), map.get(Constants.REGION), map.get(Constants.ACCESSKEY), map.get(Constants.SECRETKEY));
        load(path);
        return true;
    }

    public boolean loadAndShow(Map<String, String> map, boolean azure){
        if (!azure)
            return false;
        AzureStorageReader azureStorageReader = new AzureStorageReader();
        Path path = azureStorageReader.read(map.get(Constants.CONNECTION_STRING), map.get(Constants.CONTAINER), map.get(Constants.BLOB));
        load(path);
        return true;
    }

    public boolean loadAndShow(Path path){
        return load(path);
    }

    public boolean loadAndShow(){
        FileChooser filechooser = new FileChooser();
        File selectedFile = filechooser.showOpenDialog(stage);
        String absolutePath = selectedFile.getAbsolutePath();
        Path path = new Path(absolutePath);
        return load(path);
    }

    public List<List<String>> getData(){
        return VirtualDB.getInstance().getCommonData().getData();
    }

    public void refreshTable(){
        refreshTable(showingList);
    }

    public void refreshTable(List<String> showingList){
        CommonData commonData = VirtualDB.getInstance().getCommonData();
        TableMeta tableMeta = VirtualDB.getInstance().getTableMeta();
        tableRenderer.refresh(showingList, commonData.getPropertyList(), tableMeta.getRow(), tableMeta.getColumn(), commonData.getData());
    }

}

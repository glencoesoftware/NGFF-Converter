package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.bioformats2raw.Downsampling;
import com.glencoesoftware.bioformats2raw.ZarrCompression;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.google.common.base.Splitter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import ome.xml.model.enums.DimensionOrder;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.UnaryOperator;

import static java.util.stream.Collectors.joining;

public class CreateNGFF extends BaseTask{

    public final Converter converter = new Converter();

    private final Class<?>[] allReaders = converter.getExtraReaders();

    private ArrayList<Node> standardSettings;
    private ArrayList<Node> advancedSettings;
    private ChoiceBox<String> logLevel;
    private TextField maxWorkers;
    private ChoiceBox<ZarrCompression> compression;
    private TextField tileHeight;
    private TextField tileWidth;
    private TextField resolutions;
    private TextField series;
    private ChoiceBox<DimensionOrder> dimensionOrder;
    private ChoiceBox<Downsampling> downsampling;
    private TextField minImageSize;
    private CheckBox useExistingResolutions;
    private TextField chunkDepth;
    private TextField scaleFormatString;
    private TextField scaleFormatCSV;
    private TextField fillValue;
    private VBox compressionPropertiesBox;
    private ChoiceBox<String> compressorBloscCname;
    private TextField compressorBloscClevel;
    private TextField compressorBloscBlockSize;
    private ChoiceBox<String> compressorBloscShuffle;
    private TextField compressorZlibLevel;

    private TextField maxCachedTiles;
    private CheckBox disableMinMax;
    private CheckBox disableHCS;
    private CheckBox nested;
    private CheckBox noOMEMeta;
    private CheckBox noRoot;

    private TextField pyramidName;
    private CheckBox keepMemos;
    private TextField memoDirectory;

    private TextField readerOptions;
    private TextField outputOptions;

    private ListView<Class<?>> extraReaders;
    private final HashSet<Class<?>> desiredReaders = new HashSet<>();

    public CreateNGFF(BaseWorkflow parent) {
        super(parent);
    }

    public String getName() {
        return "Convert to NGFF";
    }

    public void setOutput(String basePath) {
        this.output = Paths.get(basePath, this.outputName + ".zarr").toFile();
    }

    private void setupIO() {
        converter.setInputPath(this.input.getAbsolutePath());
        converter.setOutputPath(this.output.getAbsolutePath());
    }

    public void updateStatus() {
        if (this.status == taskStatus.COMPLETED) { return; }
        if (this.output == null | this.input == null) {
            this.status = taskStatus.ERROR;
            this.warningMessage = "I/O not configured";
        } else {
            this.status = taskStatus.PENDING;
        }
    }

    public void run() {
        // Apply GUI configurations first
        setupIO();
        this.status = taskStatus.RUNNING;
        try {
            int result = converter.call();
            if (result == 0) {
                this.status = taskStatus.COMPLETED;
            } else {
                this.status = taskStatus.FAILED;
            }
        } catch (Exception e) {
            System.out.println("Error");
            this.status = taskStatus.FAILED;
        }
    }

    private void generateNodes() {
        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([1-9][0-9]*)?")) {
                return change;
            }
            return null;
        };

        UnaryOperator<TextFormatter.Change> integerFilterZero = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([0-9]+)?")) {
                return change;
            }
            return null;
        };

        UnaryOperator<TextFormatter.Change> integerFilterSingleDigit = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("([0-9])?")) {
                return change;
            }
            return null;
        };


        standardSettings = new ArrayList<>();
        advancedSettings = new ArrayList<>();
        logLevel = new ChoiceBox<>(FXCollections.observableArrayList("OFF", "ERROR", "WARN",
                "INFO", "DEBUG", "TRACE", "ALL"));
        HBox logSettings = new HBox(5, new Label("Log Level"), logLevel);
        logSettings.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(logSettings);

        maxWorkers = new TextField();
        maxWorkers.setTextFormatter(new TextFormatter<>(integerFilter));
        HBox workerBox = new HBox(5, new Label("Max Workers"), maxWorkers);
        workerBox.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(workerBox);

        compression = new ChoiceBox<>();
        compression.setItems(FXCollections.observableArrayList( ZarrCompression.values()));
        HBox compressionBox = new HBox(5, new Label("Compression Type"), compression);
        compressionBox.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(compressionBox);

        tileHeight = new TextField();
        tileHeight.setTextFormatter(new TextFormatter<>(integerFilter));
        HBox tileHeightBox = new HBox(5, new Label("Tile Height"), tileHeight);
        tileHeightBox.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(tileHeightBox);

        tileWidth = new TextField();
        tileWidth.setTextFormatter(new TextFormatter<>(integerFilter));
        HBox tileWidthBox = new HBox(5, new Label("Tile Width"), tileWidth);
        tileWidthBox.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(tileWidthBox);

        resolutions = new TextField();
        resolutions.setTextFormatter(new TextFormatter<>(integerFilter));
        HBox resolutionsBox = new HBox(5, new Label("Resolutions"), resolutions);
        resolutionsBox.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(resolutionsBox);

        series = new TextField();
        series.setTooltip(new Tooltip("Comma-separated list of series indexes to convert"));
        HBox seriesBox = new HBox(5, new Label("Series"), series);
        seriesBox.setAlignment(Pos.CENTER_LEFT);
        standardSettings.add(seriesBox);




        dimensionOrder = new ChoiceBox<>();
        dimensionOrder.setItems(FXCollections.observableArrayList( DimensionOrder.values()));
        HBox dimensionBox = new HBox(5, new Label("Dimension Order"), dimensionOrder);
        dimensionBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(dimensionBox);

        downsampling = new ChoiceBox<>();
        downsampling.setItems(FXCollections.observableArrayList( Downsampling.values()));
        HBox downsampleBox = new HBox(5, new Label("Downsampling Type"), downsampling);
        downsampleBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(downsampleBox);

        minImageSize = new TextField();
        minImageSize.setTextFormatter(new TextFormatter<>(integerFilter));
        minImageSize.setTooltip(new Tooltip("""
                Specifies the desired size for the largest XY
                dimension of the smallest resolution, when
                calculating the number of resolutions to generate.
                If the target size cannot be matched exactly,
                the largest XY dimension of the smallest
                resolution should be smaller than the target size."""));
        HBox minImageSizeBox = new HBox(5, new Label("Minimum Resolution Size"), minImageSize);
        minImageSizeBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(minImageSizeBox);

        useExistingResolutions = new CheckBox("Use Existing Resolutions");
        useExistingResolutions.setTooltip(new Tooltip("""
                Use existing sub resolutions from original input format.
                [Will break compatibility with raw2ometiff]
                """));
        advancedSettings.add(useExistingResolutions);

        chunkDepth = new TextField();
        chunkDepth.setTextFormatter(new TextFormatter<>(integerFilterZero));
        chunkDepth.setTooltip(new Tooltip("Maximum chunk depth to read"));
        HBox chunkDepthBox = new HBox(5, new Label("Maximum Chunk Depth"), chunkDepth);
        chunkDepthBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(chunkDepthBox);

        scaleFormatString = new TextField();
        scaleFormatString.setTooltip(new Tooltip(
                """
                         Format string for scale paths; the first two
                         arguments will always be series and resolution
                         followed by any additional arguments brought in
                         from the Scale Format CSV.
                        [Can break compatibility with raw2ometiff]
                        """));
        HBox scaleStringBox = new HBox(5, new Label("Scale Format String"), scaleFormatString);
        scaleStringBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(scaleStringBox);


        scaleFormatCSV = new TextField();
        scaleFormatCSV.setEditable(false);
        scaleFormatCSV.onMouseClickedProperty().set(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose Scale Format CSV File");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File selectedFile = fileChooser.showOpenDialog(parent.controller.jobList.getScene().getWindow());
            if (selectedFile != null) {
                scaleFormatCSV.setText(selectedFile.getAbsolutePath());
            }
        });
        HBox scaleCSVBox = new HBox(5, new Label("Scale Format CSV"), scaleFormatCSV);
        scaleCSVBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(scaleCSVBox);

        fillValue = new TextField();
        fillValue.setTextFormatter(new TextFormatter<>(integerFilterZero));
        HBox fillValueBox = new HBox(5, new Label("Fill Value"), fillValue);
        fillValueBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(fillValueBox);

        maxCachedTiles = new TextField();
        maxCachedTiles.setTextFormatter(new TextFormatter<>(integerFilter));
        HBox cachedTilesBox = new HBox(5, new Label("Max Cached Tiles"), maxCachedTiles);
        cachedTilesBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(cachedTilesBox);



        disableMinMax = new CheckBox("Calculate min/max values");
        disableMinMax.setTooltip(new Tooltip("""
                Whether to calculate minimum and maximum pixel
                values. Min/max calculation can result in slower
                conversions. If true, min/max values are saved
                as OMERO rendering metadata (true by default)"""));
        advancedSettings.add(disableMinMax);

        disableHCS = new CheckBox("Disable HCS writing");
        disableHCS.setTooltip(new Tooltip("Turn off HCS writing"));
        advancedSettings.add(disableHCS);

        nested = new CheckBox("Nested structure");
        nested.setTooltip(new Tooltip("""
                Whether to use '/' as the chunk path separator.
                Has the added effect of making blocks appear as
                subdirectories when viewed in your OS file browser.
                """));
        advancedSettings.add(nested);

        noOMEMeta = new CheckBox("Disable OME metadata");
        noOMEMeta.setTooltip(new Tooltip("""
                Turn off OME metadata exporting
                [Will break compatibility with raw2ometiff]
                """));
        advancedSettings.add(noOMEMeta);

        noRoot = new CheckBox("Disable root group");
        noRoot.setTooltip(new Tooltip("""
                Turn off creation of root group and corresponding metadata
                [Will break compatibility with raw2ometiff]
                """));
        advancedSettings.add(noRoot);

        pyramidName = new TextField();
        pyramidName.setTooltip(new Tooltip("""
                Name of pyramid
                [Can break compatibility with raw2ometiff]
                """));
        HBox pyramidNameBox = new HBox(5, new Label("Pyramid name"), pyramidName);
        pyramidNameBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(pyramidNameBox);

        keepMemos = new CheckBox("Keep memo files");
        keepMemos.setTooltip(new Tooltip("""
                Do not delete .bfmemo files created during conversion
                """));
        advancedSettings.add(keepMemos);

        memoDirectory = new TextField();
        memoDirectory.setEditable(false);
        memoDirectory.setTooltip(new Tooltip("Directory used to store .bfmemo cache files"));
        memoDirectory.onMouseClickedProperty().set(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose memo file directory");
            File newDir = directoryChooser.showDialog(parent.controller.jobList.getScene().getWindow());
            if (newDir != null) {
                memoDirectory.setText(newDir.getAbsolutePath());
            }
        });
        HBox memoDirectoryBox = new HBox(5, new Label("Memo file directory"), memoDirectory);
        memoDirectoryBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(memoDirectoryBox);


        compressionPropertiesBox = new VBox(5);
        compressionPropertiesBox.setPadding(new Insets(5));
        compressionPropertiesBox.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        advancedSettings.add(compressionPropertiesBox);

        Label compressionTitle = new Label("Compression Properties");
        compressionTitle.setFont(Font.font("ARIEL", FontWeight.BOLD, 12));
        Hyperlink compressionHelpText = new Hyperlink("Help");
        compressionHelpText.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI(
                        "https://jzarr.readthedocs.io/en/latest/tutorial.html#compressors"));
            } catch (IOException | URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
        });

        compressorBloscCname = new ChoiceBox<>(FXCollections.observableArrayList(
                "zstd", "blosclz", "lz4", "lz4hc", "zlib"));
        HBox bloscCnameBox = new HBox(5, new Label("cname"), compressorBloscCname);
        bloscCnameBox.setAlignment(Pos.CENTER_LEFT);


        compressorBloscClevel = new TextField();
        compressorBloscClevel.setTextFormatter(new TextFormatter<>(integerFilterSingleDigit));
        HBox bloscLevelBox = new HBox(5, new Label("clevel"), compressorBloscClevel);
        bloscLevelBox.setAlignment(Pos.CENTER_LEFT);

        compressorBloscBlockSize = new TextField();
        compressorBloscBlockSize.setTextFormatter(new TextFormatter<>(integerFilter));
        HBox bloscSizeBox = new HBox(5, new Label("blocksize"), compressorBloscBlockSize);
        bloscSizeBox.setAlignment(Pos.CENTER_LEFT);


        compressorBloscShuffle = new ChoiceBox<>(FXCollections.observableArrayList(
                "-1 (AUTOSHUFFLE)", "0 (NOSHUFFLE)", "1 (BYTESHUFFLE)", "2 (BITSHUFFLE)"));
        HBox bloscShuffleBox = new HBox(5, new Label("shuffle"), compressorBloscShuffle);
        bloscShuffleBox.setAlignment(Pos.CENTER_LEFT);

        compressorZlibLevel = new TextField();
        compressorZlibLevel.setTextFormatter(new TextFormatter<>(integerFilterSingleDigit));
        HBox zlibLevelBox = new HBox(5, new Label("level"), compressorZlibLevel);
        zlibLevelBox.setAlignment(Pos.CENTER_LEFT);

        compression.getSelectionModel().selectedIndexProperty().addListener(
                (observableValue, number, number2) -> {
                    System.out.println("Detected " + observableValue + number + "  " + number2);
                    compressionPropertiesBox.getChildren().clear();
                    switch (compression.getItems().get((Integer) number2)) {
                        case blosc -> {
                            compressionPropertiesBox.getChildren().addAll(compressionTitle, bloscCnameBox,
                                    bloscLevelBox, bloscSizeBox, bloscShuffleBox, compressionHelpText);
                            compressionPropertiesBox.setVisible(true);
                        }
                        case zlib -> {
                            compressionPropertiesBox.getChildren().addAll(compressionTitle, zlibLevelBox,
                                    compressionHelpText);
                            compressionPropertiesBox.setVisible(true);
                        }
                        case raw -> compressionPropertiesBox.setVisible(false);
                        default -> System.out.println("Invalid compression type");
                    }

                });

        readerOptions = new TextField();
        readerOptions.setTooltip(new Tooltip("""
                Reader-specific options, in format
                key=value,key2=value2
                """));
        HBox readerOptionsBox = new HBox(5, new Label("Reader options"), readerOptions);
        readerOptionsBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(readerOptionsBox);

        outputOptions = new TextField();
        outputOptions.setTooltip(new Tooltip("""
                Key-value pairs to be used as
                an additional argument to Filesystem
                implementations if used. In format
                key=value,key2=value2
                 For example,
                s3fs_path_style_access=true
                might be useful for connecting to minio.
                """));
        HBox outputOptionsBox = new HBox(5, new Label("Output options"), outputOptions);
        outputOptionsBox.setAlignment(Pos.CENTER_LEFT);
        advancedSettings.add(outputOptionsBox);


        extraReaders = new ListView<>();

        extraReaders.setCellFactory(list -> new CheckBoxListCell<>(item -> {
            BooleanProperty observable = new SimpleBooleanProperty(desiredReaders.contains(item));
            observable.addListener((obs, wasSelected, isNowSelected) -> {
                        if (isNowSelected) desiredReaders.add(item);
                        else desiredReaders.remove(item);
                    }
            );
            return observable;
        }) {
            @Override
            public void updateItem(Class<?> item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    String name = item.getName();
                    setText(name.substring(name.lastIndexOf(".") + 1));
                }
            }
        });
        desiredReaders.addAll(FXCollections.observableArrayList(allReaders));

        VBox extraReadersBox = new VBox(5, new Label("Extra Readers"), extraReaders);
        extraReadersBox.setPadding(new Insets(5));
        extraReadersBox.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID,
                CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        advancedSettings.add(extraReadersBox);



        // Todo: Pull overwrite from output task


    }

    private void updateNodes() {
        logLevel.setValue("WARN");
        maxWorkers.setText(String.valueOf(converter.getMaxWorkers()));
        compression.setValue(converter.getCompression());
        tileHeight.setText(String.valueOf(converter.getTileHeight()));
        tileWidth.setText(String.valueOf(converter.getTileWidth()));
        resolutions.setText(String.valueOf(converter.getResolutions()));
        series.setText(converter.getSeriesList().stream().map(String::valueOf)
                .collect(joining(",")));
        dimensionOrder.setValue(converter.getDimensionOrder());
        downsampling.setValue(converter.getDownsampling());
        minImageSize.setText(String.valueOf(converter.getMinImageSize()));
        useExistingResolutions.setSelected(converter.getReuseExistingResolutions());
        chunkDepth.setText(String.valueOf(converter.getChunkDepth()));
        scaleFormatString.setText(converter.getScaleFormat());
        Path csvPath = converter.getAdditionalScaleFormatCSV();
        if (csvPath == null) {
            scaleFormatCSV.setText(null);
        } else {
            scaleFormatCSV.setText(csvPath.toString());
        }
        fillValue.setText(String.valueOf(converter.getFillValue()));
        Map<String, Object> compressionProps = converter.getCompressionProperties();
        if (compressionProps.containsKey("cname")) {
            compressorBloscCname.setValue((String) compressionProps.get("cname"));
        } else {
            compressorBloscCname.setValue(null);
        }
        if (compressionProps.containsKey("blocksize")) {
            compressorBloscBlockSize.setText((String) compressionProps.get("blocksize"));
        } else {
            compressorBloscBlockSize.setText(null);
        }
        if (compressionProps.containsKey("clevel")) {
            compressorBloscClevel.setText((String) compressionProps.get("clevel"));
        } else {
            compressorBloscClevel.setText(null);
        }
        if (compressionProps.containsKey("shuffle")) {
            // Auto = -1, No = 0, Byte = 1, Bit = 2. So add 1 to get an index for the choicebox
            compressorBloscShuffle.getSelectionModel().select((int) compressionProps.get("shuffle") + 1);
        } else {
            compressorBloscShuffle.setValue(null);
        }
        if (compressionProps.containsKey("level")) {
            compressorZlibLevel.setText((String) compressionProps.get("level"));
        } else {
            compressorZlibLevel.setText(null);
        }

        maxCachedTiles.setText(String.valueOf(converter.getMaxCachedTiles()));
        disableMinMax.setSelected(converter.getCalculateOMEROMetadata());
        disableHCS.setSelected(converter.getNoHCS());
        nested.setSelected(converter.getNested());
        noOMEMeta.setSelected(converter.getNoOMEMeta());
        noRoot.setSelected(converter.getNoRootGroup());
        pyramidName.setText(converter.getPyramidName());
        keepMemos.setSelected(converter.getKeepMemoFiles());
        File memoDir = converter.getMemoDirectory();
        if (memoDir == null) {
            memoDirectory.setText(null);
        } else {
            memoDirectory.setText(memoDir.getAbsolutePath());
        }
        readerOptions.setText(String.join(",", converter.getReaderOptions()));
        Map<String, String> outputOpts = converter.getOutputOptions();
        if (outputOpts == null) {
            outputOptions.setText(null);
        } else {
            outputOptions.setText(outputOpts.entrySet()
                    .stream()
                    .map(Object::toString)
                    .collect(joining(",")));
        }
        extraReaders.setItems(FXCollections.observableArrayList(allReaders));
        extraReaders.setMinHeight(extraReaders.getItems().size() * 24 + 10);
        extraReaders.layout();
    }

    public ArrayList<Node> getStandardSettings() {
        if (this.standardSettings == null) {
            generateNodes();
        }
        updateNodes();
        return this.standardSettings;
    }
    public ArrayList<Node> getAdvancedSettings() {

        if (this.advancedSettings == null) {
            generateNodes();
        }
        return this.advancedSettings;
    }


    public void applySettings() {
        System.out.println("Applying settings for " + getName());
        converter.setLogLevel(logLevel.getValue());
        converter.setMaxWorkers(Integer.parseInt(maxWorkers.getText()));
        converter.setCompression(compression.getValue());
        converter.setTileHeight(Integer.parseInt(tileHeight.getText()));
        converter.setTileWidth(Integer.parseInt(tileWidth.getText()));

        if (!resolutions.getText().isEmpty()) {
            converter.setResolutions(Integer.parseInt(resolutions.getText()));
        }
        if (series.getText().isEmpty()) {
            converter.setSeriesList(Arrays.stream(series.getText().split(",")).map(Integer::parseInt).toList());
        }
        converter.setDimensionOrder(dimensionOrder.getValue());
        converter.setDownsampling(downsampling.getValue());
        converter.setMinImageSize(Integer.parseInt(minImageSize.getText()));
        converter.setReuseExistingResolutions(useExistingResolutions.isSelected());
        converter.setChunkDepth(Integer.parseInt(chunkDepth.getText()));
        converter.setScaleFormat(scaleFormatString.getText());
        if (scaleFormatCSV.getText().isEmpty()) {
            converter.setAdditionalScaleFormatCSV(Paths.get(scaleFormatCSV.getText()));
        }
        if (fillValue.getText().isEmpty()) {
            converter.setFillValue(Short.valueOf(fillValue.getText()));
        }
        Map<String, Object> compressionProps = new HashMap<>();
        switch (compression.getValue()) {
            case blosc -> {
                if (compressorBloscCname.getValue() != null) {
                    compressionProps.put("cname", compressorBloscCname.getValue());
                }
                if (compressorBloscClevel.getText().isEmpty()) {
                    compressionProps.put("clevel", compressorBloscClevel.getText());
                }
                if (compressorBloscBlockSize.getText().isEmpty()) {
                    compressionProps.put("blocksize", compressorBloscBlockSize.getText());
                }
                if (compressorBloscShuffle.getValue() != null) {
                    compressionProps.put("shuffle", compressorBloscShuffle.getValue());
                }
            }
            case zlib -> {
                if (compressorZlibLevel.getText().isEmpty()) {
                    compressionProps.put("level", compressorZlibLevel.getText());
                }
            }

        }
        converter.setCompressionProperties(compressionProps);

        converter.setMaxCachedTiles(Integer.parseInt(maxCachedTiles.getText()));
        converter.setCalculateOMEROMetadata(disableMinMax.isSelected());
        converter.setNoHCS(disableHCS.isSelected());

        converter.setUnnested(!nested.isSelected());
        converter.setNoOMEMeta(noOMEMeta.isSelected());
        converter.setNoRootGroup(noRoot.isSelected());
        if (pyramidName.getText().isEmpty()) {
            converter.setPyramidName(pyramidName.getText());
        }
        converter.setKeepMemoFiles(keepMemos.isSelected());
        if (memoDirectory.getText().isEmpty()) {
            converter.setMemoDirectory(new File(memoDirectory.getText()));
        }
        if (readerOptions.getText().isEmpty()) {
            converter.setReaderOptions(Arrays.stream(readerOptions.getText().split(",")).toList());
        }
        if (outputOptions.getText().isEmpty()) {
            converter.setOutputOptions(Splitter.on(",")
                    .withKeyValueSeparator("=")
                    .split(outputOptions.getText()));
        }
        converter.setExtraReaders(desiredReaders.toArray(new Class<?>[0]));
    }

    public void setDefaults() {
        return;
    }

    public void applyDefaults() {
        return;
    }

    public void cloneValues(BaseTask sourceInstance) {
        if (!(sourceInstance instanceof CreateNGFF source)) {
            System.out.println("Incorrect input type");
            return;
        }
        if (this.standardSettings == null) {
            generateNodes();
        }
        logLevel.setValue(source.logLevel.getValue());
        maxWorkers.setText(source.maxWorkers.getText());
        compression.setValue(source.compression.getValue());
        tileHeight.setText(source.tileHeight.getText());
        tileWidth.setText(source.tileWidth.getText());
        resolutions.setText(source.resolutions.getText());
        series.setText(source.series.getText());
        dimensionOrder.setValue(source.dimensionOrder.getValue());
        downsampling.setValue(source.downsampling.getValue());
        minImageSize.setText(source.minImageSize.getText());
        useExistingResolutions.setSelected(source.useExistingResolutions.isSelected());
        chunkDepth.setText(source.chunkDepth.getText());
        scaleFormatString.setText(source.scaleFormatString.getText());
        scaleFormatCSV.setText(source.scaleFormatCSV.getText());
        fillValue.setText(source.fillValue.getText());
        compressorBloscCname.setValue(source.compressorBloscCname.getValue());
        compressorBloscClevel.setText(source.compressorBloscClevel.getText());
        compressorBloscBlockSize.setText(source.compressorBloscBlockSize.getText());
        compressorBloscShuffle.setValue(source.compressorBloscShuffle.getValue());
        compressorZlibLevel.setText(source.compressorZlibLevel.getText());
        maxCachedTiles.setText(source.maxCachedTiles.getText());
        disableMinMax.setSelected(source.disableMinMax.isSelected());
        disableHCS.setSelected(source.disableHCS.isSelected());
        nested.setSelected(source.nested.isSelected());
        noOMEMeta.setSelected(source.noOMEMeta.isSelected());
        noRoot.setSelected(source.noRoot.isSelected());
        pyramidName.setText(source.pyramidName.getText());
        keepMemos.setSelected(source.keepMemos.isSelected());
        memoDirectory.setText(source.memoDirectory.getText());
        readerOptions.setText(source.readerOptions.getText());
        outputOptions.setText(source.outputOptions.getText());
        extraReaders.setItems(source.extraReaders.getItems());
        desiredReaders.clear();
        desiredReaders.addAll(source.desiredReaders);
    }



}

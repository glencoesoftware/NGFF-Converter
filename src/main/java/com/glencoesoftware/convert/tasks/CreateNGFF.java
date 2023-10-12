package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.bioformats2raw.Downsampling;
import com.glencoesoftware.bioformats2raw.ZarrCompression;
import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.workflows.BaseWorkflow;
import com.google.common.base.Splitter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import org.controlsfx.control.ToggleSwitch;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.*;
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

// Run bioformats2raw on a file
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
    private ToggleSwitch useExistingResolutions;
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
    private ToggleSwitch disableMinMax;
    private ToggleSwitch disableHCS;
    private ToggleSwitch nested;
    private ToggleSwitch noOMEMeta;
    private ToggleSwitch noRoot;

    private TextField pyramidName;
    private ToggleSwitch keepMemos;
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
        this.output = Paths.get(
                basePath, this.outputName + ".zarr").toFile();
    }

    public void setOverwrite(boolean shouldOverwrite) {
        converter.setOverwrite(shouldOverwrite);
    }

    private void setupIO() {
        converter.setInputPath(this.input.getAbsolutePath());
        converter.setOutputPath(this.output.getAbsolutePath());
    }

    public void updateStatus() {
        if (this.status == JobState.status.COMPLETED) { return; }
        if (this.output == null | this.input == null) {
            this.status = JobState.status.WARNING;
            this.warningMessage = "I/O not configured";
            return;
        }
        if (this.output.exists() & !converter.getOverwrite()) {
            this.status = JobState.status.WARNING;
            this.warningMessage = "NGFF file %s already exists".formatted(this.output.getName());
            return;
        }
        this.status = JobState.status.READY;
    }

    public void run() {
        // Apply GUI configurations first
        setupIO();
        LOGGER.info("Running bioformats2raw");
        this.status = JobState.status.RUNNING;
        try {
            int result = converter.call();
            if (result == 0) {
                this.status = JobState.status.COMPLETED;
                LOGGER.info("NGFF creation complete");
            } else {
                this.status = JobState.status.FAILED;
                LOGGER.error("NGFF creation failed with exit code %d".formatted(result));

            }
        } catch (Exception e) {
            LOGGER.error("NGFF creation failed - " + e);
            this.status = JobState.status.FAILED;
        }
    }

    public void generateNodes() {
        if (standardSettings != null) return;
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
        standardSettings.add(getSettingContainer(
                logLevel,
                "Log Level",
                "Detail level of logs to record"
        ));

        maxWorkers = new TextField();
        maxWorkers.setTextFormatter(new TextFormatter<>(integerFilter));
        standardSettings.add(getSettingContainer(
                maxWorkers,
                "Max Workers",
                "Maximum number of worker processes to use for this task"
        ));

        compression = new ChoiceBox<>();
        compression.setItems(FXCollections.observableArrayList( ZarrCompression.values()));
        standardSettings.add(getSettingContainer(
                compression,
                "Compression Type",
                """
                        Type of compression to use with the image.
                        
                        blosc - Fast, lossless compression (recommended)
                        zlib - Alternative lossless compression
                        null/raw - No compression. Will increase file size
                        """
        ));

        tileHeight = new TextField();
        tileHeight.setTextFormatter(new TextFormatter<>(integerFilter));
        standardSettings.add(getSettingContainer(
                tileHeight,
                "Tile height",
                """
                Maximum tile height (default: 1024). This is both
                the chunk size (in Y) when writing Zarr and the
                tile size used for reading from the original
                data. Changing the tile size may have
                performance implications.
                """
        ));

        tileWidth = new TextField();
        tileWidth.setTextFormatter(new TextFormatter<>(integerFilter));
        standardSettings.add(getSettingContainer(
                tileWidth,
                "Tile width",
                """
                        Maximum tile width (default: 1024).
                        This is both the chunk size (in X) when writing
                        Zarr and the tile size used for reading from the
                        original data. Changing the tile size may have
                        performance implications.
                        """

        ));

        resolutions = new TextField();
        resolutions.setTextFormatter(new TextFormatter<>(integerFilter));
        standardSettings.add(getSettingContainer(
                resolutions,
                "Resolutions",
                "Number of pyramid resolutions to generate"
        ));

        series = new TextField();
        standardSettings.add(getSettingContainer(
                series,
                "Series",
                "Comma-separated list of series indexes to convert"
        ));

        /* Begin advanced settings */

        dimensionOrder = new ChoiceBox<>();
        dimensionOrder.setItems(FXCollections.observableArrayList( DimensionOrder.values()));
        advancedSettings.add(getSettingContainer(
                dimensionOrder,
                "Dimension Order",
                """
                Override the input file dimension order in the
                output file.
                [Can break compatibility with raw2ometiff]
                """
        ));

        downsampling = new ChoiceBox<>();
        downsampling.setItems(FXCollections.observableArrayList( Downsampling.values()));
        advancedSettings.add(getSettingContainer(
                downsampling,
                "Downsampling Type",
                "Tile downsampling algorithm used for pyramid resolutions"
        ));

        minImageSize = new TextField();
        minImageSize.setTextFormatter(new TextFormatter<>(integerFilter));
        advancedSettings.add(getSettingContainer(
                minImageSize,
                "Minimum Resolution Size",
                """
                Specifies the desired size for the largest XY
                dimension of the smallest resolution, when
                calculating the number of resolutions to generate.
                If the target size cannot be matched exactly,
                the largest XY dimension of the smallest
                resolution should be smaller than the target size.
                """
        ));

        useExistingResolutions = new ToggleSwitch();
        advancedSettings.add(getSettingContainer(
                useExistingResolutions,
                "Use Existing Resolutions",
                """
                Use existing sub resolutions from original input format.
                [Will break compatibility with raw2ometiff]
                """
        ));

        chunkDepth = new TextField();
        chunkDepth.setTextFormatter(new TextFormatter<>(integerFilterZero));
        advancedSettings.add(getSettingContainer(
                chunkDepth,
                "Maximum Chunk Depth",
                "Maximum chunk depth to read"
        ));

        scaleFormatString = new TextField();
        advancedSettings.add(getSettingContainer(
                scaleFormatString,
                "Scale Format String",
                """
                         Format string for scale paths; the first two
                        arguments will always be series and resolution
                        followed by any additional arguments brought in
                        from the Scale Format CSV.
                        [Can break compatibility with raw2ometiff]
                        """
        ));

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
        advancedSettings.add(getSettingContainer(
                scaleFormatCSV,
                "Scale Format CSV",
                """
                        Additional format string argument CSV file
                        (without header row).  Arguments will be added
                        to the end of the scale format string mapping
                        the at the corresponding CSV row index.  It is
                        expected that the CSV file contain exactly the
                        same number of rows as the input file has series
                        """
        ));

        fillValue = new TextField();
        fillValue.setTextFormatter(new TextFormatter<>(integerFilterZero));
        advancedSettings.add(getSettingContainer(
                fillValue,
                "Fill Value",
                """
                        Default value to fill in for missing tiles (0-255)
                        (currently .mrxs only)
                        """
        ));

        maxCachedTiles = new TextField();
        maxCachedTiles.setTextFormatter(new TextFormatter<>(integerFilter));
        advancedSettings.add(getSettingContainer(
                maxCachedTiles,
                "Max Cached Tiles",
                """
                        Maximum number of tiles that will be cached across
                        all workers
                        """
        ));

        disableMinMax = new ToggleSwitch();
        advancedSettings.add(getSettingContainer(
                disableMinMax,
                "Calculate min/max values",
                """
                Whether to calculate minimum and maximum pixel
                values. Min/max calculation can result in slower
                conversions. If true, min/max values are saved
                as OMERO rendering metadata
                """
        ));

        disableHCS = new ToggleSwitch();
        advancedSettings.add(getSettingContainer(
                disableHCS,
                "Disable HCS Writing",
                "Turn off HCS writing"
        ));

        nested = new ToggleSwitch();
        advancedSettings.add(getSettingContainer(
                nested,
                "Nested Structure",
                """
                Whether to use '/' as the chunk path separator.
                Has the added effect of making blocks appear as
                subdirectories when viewed in your OS file browser.
                """
        ));

        noOMEMeta = new ToggleSwitch();
        advancedSettings.add(getSettingContainer(
                noOMEMeta,
                "Disable OME metadata",
                """
                Turn off OME metadata exporting
                [Will break compatibility with raw2ometiff]
                """
        ));

        noRoot = new ToggleSwitch();
        advancedSettings.add(getSettingContainer(
                noRoot,
                "Disable root group",
                """
                Turn off creation of root group and corresponding metadata
                [Will break compatibility with raw2ometiff]
                """
        ));

        pyramidName = new TextField();
        advancedSettings.add(getSettingContainer(
                pyramidName,
                "Pyramid name",
                """
                Name of pyramid
                [Can break compatibility with raw2ometiff]
                """
        ));

        keepMemos = new ToggleSwitch();
        advancedSettings.add(getSettingContainer(
                keepMemos,
                "Keep Memo Files",
                "Do not delete .bfmemo files created during conversion"
        ));

        memoDirectory = new TextField();
        memoDirectory.setEditable(false);
        memoDirectory.onMouseClickedProperty().set(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose memo file directory");
            File newDir = directoryChooser.showDialog(parent.controller.jobList.getScene().getWindow());
            if (newDir != null) {
                memoDirectory.setText(newDir.getAbsolutePath());
            }
        });
        advancedSettings.add(getSettingContainer(
                memoDirectory,
                "Memo file directory",
                "Directory used to store .bfmemo cache files"
        ));

        compressionPropertiesBox = getSettingGroupContainer();
        Label compressionTitle = getSettingHeader("Compression Properties",
                "Parameters to be supplied to the compression algorithm");
        advancedSettings.add(compressionPropertiesBox);

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
        VBox bloscCnameBox = getSettingContainer(
                compressorBloscCname,
                "cname",
                ""
        );

        compressorBloscClevel = new TextField();
        compressorBloscClevel.setTextFormatter(new TextFormatter<>(integerFilterSingleDigit));
        VBox bloscLevelBox = getSettingContainer(
                compressorBloscClevel,
                "clevel",
                ""
        );

        compressorBloscBlockSize = new TextField();
        compressorBloscBlockSize.setTextFormatter(new TextFormatter<>(integerFilter));
        VBox bloscSizeBox = getSettingContainer(
                compressorBloscBlockSize,
                "blocksize",
                ""
        );

        compressorBloscShuffle = new ChoiceBox<>(FXCollections.observableArrayList(
                "-1 (AUTOSHUFFLE)", "0 (NOSHUFFLE)", "1 (BYTESHUFFLE)", "2 (BITSHUFFLE)"));
        VBox bloscShuffleBox = getSettingContainer(
                compressorBloscShuffle,
                "shuffle",
                ""
        );

        compressorZlibLevel = new TextField();
        compressorZlibLevel.setTextFormatter(new TextFormatter<>(integerFilterSingleDigit));
        VBox zlibLevelBox = getSettingContainer(
                compressorZlibLevel,
                "level",
                ""
        );

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
        advancedSettings.add(getSettingContainer(
                readerOptions,
                "Reader Options",
                """
                        Reader-specific options, in format
                        key=value,key2=value2
                        """
        ));

        outputOptions = new TextField();
        advancedSettings.add(getSettingContainer(
                outputOptions,
                "Output Options",
                """
                Key-value pairs to be used as
                an additional argument to Filesystem
                implementations if used. In format
                key=value,key2=value2
                For example,
                s3fs_path_style_access=true
                might be useful for connecting to minio.
                """
        ));

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

        advancedSettings.add(getSettingContainer(
                extraReaders,
                "Extra Readers",
                "Use this dialog to enable/disable extra image format readers"
        ));

    }

    public void updateNodes() {
        logLevel.setValue(converter.getLogLevel());
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
        // Todo: Solve this conflict
        fillValue.setText(String.valueOf(converter.getFillValue()));
        Map<String, Object> compressionProps = converter.getCompressionProperties();
        if (compressionProps.containsKey("cname")) {
            compressorBloscCname.setValue((String) compressionProps.get("cname"));
        } else {
            compressorBloscCname.setValue("lz4");
        }
        if (compressionProps.containsKey("blocksize")) {
            compressorBloscBlockSize.setText((String) compressionProps.get("blocksize"));
        } else {
            compressorBloscBlockSize.setText("0");
        }
        if (compressionProps.containsKey("clevel")) {
            compressorBloscClevel.setText((String) compressionProps.get("clevel"));
        } else {
            compressorBloscClevel.setText("5");
        }
        if (compressionProps.containsKey("shuffle")) {
            // Auto = -1, No = 0, Byte = 1, Bit = 2. So add 1 to get an index for the choicebox
            compressorBloscShuffle.getSelectionModel().select((int) compressionProps.get("shuffle") + 1);
        } else {
            compressorBloscShuffle.getSelectionModel().select(2);
        }
        if (compressionProps.containsKey("level")) {
            compressorZlibLevel.setText((String) compressionProps.get("level"));
        } else {
            compressorZlibLevel.setText("1");
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
        if (!series.getText().isEmpty()) {
            converter.setSeriesList(Arrays.stream(series.getText().split(",")).map(Integer::parseInt).toList());
        }
        converter.setDimensionOrder(dimensionOrder.getValue());
        converter.setDownsampling(downsampling.getValue());
        converter.setMinImageSize(Integer.parseInt(minImageSize.getText()));
        converter.setReuseExistingResolutions(useExistingResolutions.isSelected());
        converter.setChunkDepth(Integer.parseInt(chunkDepth.getText()));
        converter.setScaleFormat(scaleFormatString.getText());
        if (scaleFormatCSV.getText() != null) {
            converter.setAdditionalScaleFormatCSV(Paths.get(scaleFormatCSV.getText()));
        }
        if (!fillValue.getText().isEmpty()) {
            converter.setFillValue(Short.valueOf(fillValue.getText()));
        }
        Map<String, Object> compressionProps = getCompressionProps();
        converter.setCompressionProperties(compressionProps);

        converter.setMaxCachedTiles(Integer.parseInt(maxCachedTiles.getText()));
        converter.setCalculateOMEROMetadata(disableMinMax.isSelected());
        converter.setNoHCS(disableHCS.isSelected());

        converter.setUnnested(!nested.isSelected());
        converter.setNoOMEMeta(noOMEMeta.isSelected());
        converter.setNoRootGroup(noRoot.isSelected());
        if (pyramidName.getText() != null) {
            converter.setPyramidName(pyramidName.getText());
        }
        converter.setKeepMemoFiles(keepMemos.isSelected());
        if (memoDirectory.getText() != null) {
            converter.setMemoDirectory(new File(memoDirectory.getText()));
        }
        if (readerOptions.getText() != null) {
            converter.setReaderOptions(Arrays.stream(readerOptions.getText().split(",")).toList());
        }
        if (outputOptions.getText() != null) {
            converter.setOutputOptions(Splitter.on(",")
                    .withKeyValueSeparator("=")
                    .split(outputOptions.getText()));
        }
        converter.setExtraReaders(desiredReaders.toArray(new Class<?>[0]));

    }



    private Map<String, Object> getCompressionProps() {
        Map<String, Object> compressionProps = new HashMap<>();
        switch (compression.getValue()) {
            case blosc -> {
                if (compressorBloscCname.getValue() != null) {
                    compressionProps.put("cname", compressorBloscCname.getValue());
                }
                if (compressorBloscClevel.getText() != null) {
                    compressionProps.put("clevel", compressorBloscClevel.getText());
                }
                if (compressorBloscBlockSize.getText() != null) {
                    compressionProps.put("blocksize", compressorBloscBlockSize.getText());
                }
                if (compressorBloscShuffle.getValue() != null) {
                    // Auto = -1, No = 0, Byte = 1, Bit = 2. So subtract 1 to convert
                    compressionProps.put("shuffle", compressorBloscShuffle.getSelectionModel().getSelectedIndex() - 1);
                }
            }
            case zlib -> {
                if (compressorZlibLevel.getText() != null) {
                    compressionProps.put("level", compressorZlibLevel.getText());
                }
            }

        }
        return compressionProps;
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

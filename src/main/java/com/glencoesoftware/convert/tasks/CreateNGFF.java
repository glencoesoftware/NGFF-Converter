package com.glencoesoftware.convert.tasks;

import com.glencoesoftware.bioformats2raw.Converter;
import com.glencoesoftware.bioformats2raw.Downsampling;
import com.glencoesoftware.bioformats2raw.ZarrCompression;
import com.glencoesoftware.convert.JobState;
import com.glencoesoftware.convert.tasks.progress.NGFFProgressListener;
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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static java.util.stream.Collectors.joining;

// Run bioformats2raw on a file
public class CreateNGFF extends BaseTask{

    public Converter converter = new Converter();

    public static String name = "Convert to NGFF";

    public static final Preferences taskPreferences = Preferences.userRoot().node(name);
    public enum prefKeys {LOG_LEVEL, MAX_WORKERS, COMPRESSION, TILE_WIDTH, TILE_HEIGHT, RESOLUTIONS, SERIES,
        DIMENSION_ORDER, DOWNSAMPLING, MIN_IMAGE_SIZE, REUSE_RES, CHUNK_DEPTH, SCALE_FORMAT_STRING,
        FILL_VALUE, BLOSC_CNAME, BLOSC_CLEVEL, BLOSC_BLOCKSIZE, BLOSC_SHUFFLE, ZLIB_LEVEL,
        MAX_CACHED_TILES, MIN_MAX, HCS, NESTED, OME_META, NO_ROOT, PYRAMID_NAME, KEEP_MEMOS, MEMO_DIR,
        READER_OPTS, OUTPUT_OPTS, EXTRA_READERS
    }


    private final Class<?>[] allReaders = converter.getExtraReaders();

    private static final ArrayList<Node> standardSettings = new ArrayList<>();
    private static final ArrayList<Node> advancedSettings = new ArrayList<>();
    private static final ChoiceBox<String> logLevel;
    private static final TextField maxWorkers;
    private static final ChoiceBox<ZarrCompression> compression;
    private static final TextField tileHeight;
    private static final TextField tileWidth;
    private static final TextField resolutions;
    private static final TextField series;
    private static final ChoiceBox<DimensionOrder> dimensionOrder;
    private static final ChoiceBox<Downsampling> downsampling;
    private static final TextField minImageSize;
    private static final ToggleSwitch useExistingResolutions;
    private static final TextField chunkDepth;
    private static final TextField scaleFormatString;
    private static final TextField scaleFormatCSV;
    private static final TextField fillValue;
    private static final VBox compressionPropertiesBox;
    private static final ChoiceBox<String> compressorBloscCname;
    private static final TextField compressorBloscClevel;
    private static final TextField compressorBloscBlockSize;
    private static final ChoiceBox<String> compressorBloscShuffle;
    private static final TextField compressorZlibLevel;

    private static final TextField maxCachedTiles;
    private static final ToggleSwitch disableMinMax;
    private static final ToggleSwitch disableHCS;
    private static final ToggleSwitch nested;
    private static final ToggleSwitch noOMEMeta;
    private static final ToggleSwitch noRoot;
    private static final TextField pyramidName;
    private static final ToggleSwitch keepMemos;
    private static final TextField memoDirectory;

    private static final TextField readerOptions;
    private static final TextField outputOptions;

    private static final ListView<Class<?>> extraReaders;
    private final HashSet<Class<?>> desiredReaders = new HashSet<>();

    public String getName() { return name; }

    public CreateNGFF(BaseWorkflow parent) {
        super(parent);
        // Load the preferences stored as defaults
        applyDefaults();
    }

    // Load settings from this instance's converter into the static widgets
    public void prepareForDisplay() {
        // Link widgets to this instance
        bindWidgets();

        // Populate setting values
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
        fillValue.setText(String.valueOf(converter.getFillValue()));
        Map<String, Object> compressionProps = converter.getCompressionProperties();
        if (compressionProps.containsKey("cname")) {
            compressorBloscCname.setValue((String) compressionProps.get("cname"));
        } else {
            compressorBloscCname.setValue("lz4");
        }
        if (compressionProps.containsKey("blocksize")) {
            compressorBloscBlockSize.setText(String.valueOf(compressionProps.get("blocksize")));
        } else {
            compressorBloscBlockSize.setText("0");
        }
        if (compressionProps.containsKey("clevel")) {
            compressorBloscClevel.setText(String.valueOf(compressionProps.get("clevel")));
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
            compressorZlibLevel.setText(String.valueOf(compressionProps.get("level")));
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

    // Bind callbacks for static widgets to this specific class instance
    private void bindWidgets() {
        // Extra readers should manipulate the instance-specific desiredReaders list.
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
    }

    // Save settings from widgets into the converter's values
    public void applySettings() {
        System.out.println("Applying settings for " + name);
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

    // Duplicate settings from another supplied instance (except input/output)
    public void cloneValues(BaseTask sourceInstance) {
        if (!(sourceInstance instanceof CreateNGFF source)) {
            System.out.println("Incorrect input type");
            return;
        }
        converter.setLogLevel(source.converter.getLogLevel());
        converter.setMaxWorkers(source.converter.getMaxWorkers());
        converter.setCompression(source.converter.getCompression());
        converter.setTileHeight(source.converter.getTileHeight());
        converter.setTileWidth(source.converter.getTileWidth());
        converter.setResolutions(source.converter.getResolutions());
        converter.setSeriesList(source.converter.getSeriesList());
        converter.setDimensionOrder(source.converter.getDimensionOrder());
        converter.setDownsampling(source.converter.getDownsampling());
        converter.setMinImageSize(source.converter.getMinImageSize());
        converter.setReuseExistingResolutions(source.converter.getReuseExistingResolutions());
        converter.setChunkDepth(source.converter.getChunkDepth());
        converter.setScaleFormat(source.converter.getScaleFormat());
        converter.setAdditionalScaleFormatCSV(source.converter.getAdditionalScaleFormatCSV());
        converter.setFillValue(source.converter.getFillValue());
        converter.setCompressionProperties(source.converter.getCompressionProperties());
        converter.setMaxCachedTiles(source.converter.getMaxCachedTiles());
        converter.setCalculateOMEROMetadata(source.converter.getCalculateOMEROMetadata());
        converter.setNoHCS(source.converter.getNoHCS());
        converter.setUnnested(!source.converter.getNested());
        converter.setNoOMEMeta(source.converter.getNoOMEMeta());
        converter.setNoRootGroup(source.converter.getNoRootGroup());
        converter.setPyramidName(source.converter.getPyramidName());
        converter.setKeepMemoFiles(source.converter.getKeepMemoFiles());
        converter.setMemoDirectory(source.converter.getMemoDirectory());
        converter.setReaderOptions(source.converter.getReaderOptions());
        converter.setOutputOptions(source.converter.getOutputOptions());
        converter.setExtraReaders(source.converter.getExtraReaders());

        desiredReaders.clear();
        desiredReaders.addAll(source.desiredReaders);

        // Todo: Handle null return values

    }


    public void calculateOutput(String basePath) {
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
        // TODO: Handle existing NGFF

        System.out.println("Setting progress up");
        NGFFProgressListener listener = new NGFFProgressListener(progressBar, progressLabel, converter);
        converter.setProgressListener(listener);
        System.out.println("set progressbar listener");

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
        } finally {
            listener.stop();
        }
    }

    // Generate settings widgets
    static {
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
        FileChooser.ExtensionFilter[] filter = {
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")};

        HBox scaleFormatCSVWidget = getFileSelectWidget(scaleFormatCSV, "Choose Scale Format CSV File", false,
                null, filter);

        advancedSettings.add(getSettingContainer(
                scaleFormatCSVWidget,
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
        HBox memoWidget = getDirectorySelectWidget(memoDirectory, "Choose memo file directory", null);
        advancedSettings.add(getSettingContainer(
                memoWidget,
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

        advancedSettings.add(getSettingContainer(
                extraReaders,
                "Extra Readers",
                "Use this dialog to enable/disable extra image format readers"
        ));

    }

    public ArrayList<Node> getStandardSettings() {
        return standardSettings;
    }
    public ArrayList<Node> getAdvancedSettings() {
        return advancedSettings;
    }

    private Map<String, Object> getCompressionProps() {
        Map<String, Object> compressionProps = new HashMap<>();
        switch (compression.getValue()) {
            case blosc -> {
                if (compressorBloscCname.getValue() != null)
                    compressionProps.put("cname", compressorBloscCname.getValue());
                if (compressorBloscClevel.getText() != null)
                    compressionProps.put("clevel", Integer.parseInt(compressorBloscClevel.getText()));
                if (compressorBloscBlockSize.getText() != null && !compressorBloscBlockSize.getText().isEmpty())
                    compressionProps.put("blocksize", Integer.parseInt(compressorBloscBlockSize.getText()));
                // Auto = -1, No = 0, Byte = 1, Bit = 2. So subtract 1 to convert
                if (compressorBloscShuffle.getValue() != null)
                    compressionProps.put("shuffle", compressorBloscShuffle.getSelectionModel().getSelectedIndex() - 1);
            }
            case zlib -> {
                if (compressorZlibLevel.getText() != null)
                    compressionProps.put("level", Integer.parseInt(compressorZlibLevel.getText()));
            }
        }
        return compressionProps;
    }

    public void setDefaults() throws BackingStoreException {
        taskPreferences.clear();
        taskPreferences.put(prefKeys.LOG_LEVEL.name(), converter.getLogLevel());
        taskPreferences.putInt(prefKeys.MAX_WORKERS.name(), converter.getMaxWorkers());
        taskPreferences.put(prefKeys.COMPRESSION.name(), converter.getCompression().name());
        taskPreferences.putInt(prefKeys.TILE_HEIGHT.name(), converter.getTileHeight());
        taskPreferences.putInt(prefKeys.TILE_WIDTH.name(), converter.getTileWidth());
        if (converter.getResolutions() != null) {
            taskPreferences.putInt(prefKeys.RESOLUTIONS.name(), converter.getResolutions());
        }
        taskPreferences.put(prefKeys.SERIES.name(), converter.getSeriesList()
                .stream().map(String::valueOf).collect(joining(",")));
        taskPreferences.put(prefKeys.DIMENSION_ORDER.name(), converter.getDimensionOrder().name());
        taskPreferences.put(prefKeys.DOWNSAMPLING.name(), converter.getDownsampling().name());
        taskPreferences.putInt(prefKeys.MIN_IMAGE_SIZE.name(), converter.getMinImageSize());
        taskPreferences.putBoolean(prefKeys.REUSE_RES.name(), converter.getReuseExistingResolutions());
        taskPreferences.putInt(prefKeys.CHUNK_DEPTH.name(), converter.getChunkDepth());
        taskPreferences.put(prefKeys.SCALE_FORMAT_STRING.name(), converter.getScaleFormat());
        // For now we won't save task-specific paths as defaults?
        // taskPreferences.put(prefKeys.SCALE_FORMAT_CSV.name(), converter.getAdditionalScaleFormatCSV().toString());
        if (converter.getFillValue() != null) {
            taskPreferences.putInt(prefKeys.FILL_VALUE.name(), converter.getFillValue());
        }

        Map<String, Object> compressionProps = converter.getCompressionProperties();
        taskPreferences.remove(prefKeys.BLOSC_CNAME.name());
        taskPreferences.remove(prefKeys.BLOSC_CLEVEL.name());
        taskPreferences.remove(prefKeys.BLOSC_BLOCKSIZE.name());
        taskPreferences.remove(prefKeys.BLOSC_SHUFFLE.name());
        taskPreferences.remove(prefKeys.ZLIB_LEVEL.name());
        if (converter.getCompression() == ZarrCompression.blosc) {
            taskPreferences.put(prefKeys.BLOSC_CNAME.name(), (String) compressionProps.get("cname"));
            taskPreferences.putInt(prefKeys.BLOSC_CLEVEL.name(), (Integer) compressionProps.get("clevel"));
            taskPreferences.putInt(prefKeys.BLOSC_BLOCKSIZE.name(), (Integer) compressionProps.get("blocksize"));
            taskPreferences.putInt(prefKeys.BLOSC_SHUFFLE.name(), (Integer) compressionProps.get("shuffle"));
        } else if (converter.getCompression() == ZarrCompression.zlib) {
            taskPreferences.putInt(prefKeys.ZLIB_LEVEL.name(), (Integer) compressionProps.get("level"));
        }
        taskPreferences.putInt(prefKeys.MAX_CACHED_TILES.name(), converter.getMaxCachedTiles());

        taskPreferences.putBoolean(prefKeys.MIN_MAX.name(), converter.getCalculateOMEROMetadata());
        taskPreferences.putBoolean(prefKeys.HCS.name(), converter.getNoHCS());
        taskPreferences.putBoolean(prefKeys.NESTED.name(), converter.getNested());
        taskPreferences.putBoolean(prefKeys.OME_META.name(), converter.getNoOMEMeta());
        taskPreferences.putBoolean(prefKeys.NO_ROOT.name(), converter.getNoRootGroup());
        if (converter.getPyramidName() != null) {
            taskPreferences.put(prefKeys.PYRAMID_NAME.name(), converter.getPyramidName());
        }
        taskPreferences.putBoolean(prefKeys.KEEP_MEMOS.name(), converter.getKeepMemoFiles());
        if (converter.getMemoDirectory() != null) {
            taskPreferences.put(prefKeys.MEMO_DIR.name(), converter.getMemoDirectory().getAbsolutePath());
        }
        taskPreferences.put(prefKeys.READER_OPTS.name(), String.join(",", converter.getReaderOptions()));
        Map<String, String> outputOpts = converter.getOutputOptions();
        if (outputOpts != null) {
            taskPreferences.put(prefKeys.OUTPUT_OPTS.name(), (outputOpts.entrySet()
                    .stream()
                    .map(Object::toString)
                    .collect(joining(","))));
        }
        taskPreferences.put(prefKeys.EXTRA_READERS.name(), desiredReaders.stream().map(Class::getName).collect(joining(",")));
        taskPreferences.flush();
    }

    public void applyDefaults() {
        converter.setLogLevel(taskPreferences.get(prefKeys.LOG_LEVEL.name(), converter.getLogLevel()));
        converter.setMaxWorkers(taskPreferences.getInt(prefKeys.MAX_WORKERS.name(), converter.getMaxWorkers()));
        converter.setCompression(ZarrCompression.valueOf(
                taskPreferences.get(prefKeys.COMPRESSION.name(), converter.getCompression().name())));
        converter.setTileHeight(taskPreferences.getInt(prefKeys.TILE_HEIGHT.name(), converter.getTileHeight()));
        converter.setTileWidth(taskPreferences.getInt(prefKeys.TILE_WIDTH.name(), converter.getTileWidth()));
        if (taskPreferences.get(prefKeys.RESOLUTIONS.name(), null) != null)
            converter.setResolutions(taskPreferences.getInt(prefKeys.RESOLUTIONS.name(), 0));

        String seriesList = taskPreferences.get(prefKeys.SERIES.name(), null);
        if (seriesList != null && !seriesList.isEmpty()) {
            converter.setSeriesList(Arrays.stream(series.getText().split(",")).map(Integer::parseInt).toList());
        }

        converter.setDimensionOrder(DimensionOrder.valueOf(taskPreferences.get(prefKeys.DIMENSION_ORDER.name(),
                converter.getDimensionOrder().name())));
        converter.setDownsampling(Downsampling.valueOf(taskPreferences.get(prefKeys.DOWNSAMPLING.name(),
                converter.getDownsampling().name())));
        converter.setMinImageSize(taskPreferences.getInt(prefKeys.MIN_IMAGE_SIZE.name(), converter.getMinImageSize()));
        converter.setReuseExistingResolutions(taskPreferences.getBoolean(
                prefKeys.REUSE_RES.name(), converter.getReuseExistingResolutions()));
        converter.setChunkDepth(taskPreferences.getInt(prefKeys.CHUNK_DEPTH.name(), converter.getChunkDepth()));
        converter.setScaleFormat(taskPreferences.get(prefKeys.SCALE_FORMAT_STRING.name(), converter.getScaleFormat()));
        // Scale format CSV not saved
        if (taskPreferences.get(prefKeys.FILL_VALUE.name(), null) != null)
            converter.setFillValue((short) taskPreferences.getInt(prefKeys.FILL_VALUE.name(), 0));

        Map<String, Object> compressionProps = converter.getCompressionProperties();
        if (converter.getCompression() == ZarrCompression.blosc) {
            compressionProps.put("cname", taskPreferences.get(prefKeys.BLOSC_CNAME.name(), "lz4"));
            compressionProps.put("clevel", taskPreferences.getInt(prefKeys.BLOSC_CLEVEL.name(), 5 ));
            compressionProps.put("blocksize", taskPreferences.getInt(prefKeys.BLOSC_BLOCKSIZE.name(), 0));
            compressionProps.put("shuffle", taskPreferences.getInt(prefKeys.BLOSC_SHUFFLE.name(), 1));
        } else if (converter.getCompression() == ZarrCompression.zlib) {
            compressionProps.put("level", taskPreferences.getInt(prefKeys.ZLIB_LEVEL.name(),1));
        }
        converter.setCompressionProperties(compressionProps);

        converter.setMaxCachedTiles(taskPreferences.getInt(prefKeys.MAX_CACHED_TILES.name(),
                converter.getMaxCachedTiles()));

        converter.setCalculateOMEROMetadata(taskPreferences.getBoolean(
                prefKeys.MIN_MAX.name(), converter.getCalculateOMEROMetadata()));
        converter.setNoHCS(taskPreferences.getBoolean(prefKeys.HCS.name(), converter.getNoHCS()));
        converter.setUnnested(!taskPreferences.getBoolean(prefKeys.NESTED.name(), converter.getNested()));
        converter.setNoOMEMeta(taskPreferences.getBoolean(prefKeys.OME_META.name(), converter.getNoOMEMeta()));
        converter.setNoRootGroup(taskPreferences.getBoolean(prefKeys.NO_ROOT.name(), converter.getNoRootGroup()));
        converter.setPyramidName(taskPreferences.get(prefKeys.PYRAMID_NAME.name(), converter.getPyramidName()));
        converter.setKeepMemoFiles(taskPreferences.getBoolean(
                prefKeys.KEEP_MEMOS.name(), converter.getKeepMemoFiles()));
        String memoDir = taskPreferences.get(prefKeys.MEMO_DIR.name(), null);
        if (memoDir != null) {
            converter.setMemoDirectory(new File(memoDir));
        }

        converter.setReaderOptions(Arrays.stream(
                taskPreferences.get(prefKeys.READER_OPTS.name(),
                        String.join(",", converter.getReaderOptions())).split(",")).toList());

        String outputOpts = taskPreferences.get(prefKeys.OUTPUT_OPTS.name(), null);
        if (outputOpts != null && !outputOpts.isEmpty()) {
            converter.setOutputOptions(Splitter.on(",")
                    .withKeyValueSeparator("=")
                    .split(outputOpts));
        }

        String readers = taskPreferences.get(prefKeys.EXTRA_READERS.name(), null);
        desiredReaders.clear();
        if (readers == null) desiredReaders.addAll(FXCollections.observableArrayList(allReaders));
        else {
            desiredReaders.addAll(Arrays.stream(readers.split(",")).map((String s) -> {
                try {
                    return Class.forName(s);
                } catch (ClassNotFoundException e) {
                    LOGGER.error("Did not find class for extra reader " + s);
                    return null;
                }
            }).toList());
        }
    }

    public void resetToDefaults() {
        resetConverter();
        applyDefaults();
    }

    public void resetConverter() {
        // Todo: Revise once b2r resetters are implemented
        converter = new Converter();
    }

}

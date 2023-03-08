package gov.nasa.gsfc.seadas.imageanimator.ui;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.rcp.imgfilter.FilteredBandAction;
import org.esa.snap.rcp.imgfilter.model.Filter;

public class ImageAnimatorFilteredBandAction extends FilteredBandAction {


    static GeneralFilterBand.OpType getOpType(Filter.Operation operation) {
        if (operation == Filter.Operation.OPEN) {
            return GeneralFilterBand.OpType.OPENING;
        } else if (operation == Filter.Operation.CLOSE) {
            return GeneralFilterBand.OpType.CLOSING;
        } else if (operation == Filter.Operation.ERODE) {
            return GeneralFilterBand.OpType.EROSION;
        } else if (operation == Filter.Operation.DILATE) {
            return GeneralFilterBand.OpType.DILATION;
        } else if (operation == Filter.Operation.MIN) {
            return GeneralFilterBand.OpType.MIN;
        } else if (operation == Filter.Operation.MAX) {
            return GeneralFilterBand.OpType.MAX;
        } else if (operation == Filter.Operation.MEAN) {
            return GeneralFilterBand.OpType.MEAN;
        } else if (operation == Filter.Operation.MEDIAN) {
            return GeneralFilterBand.OpType.MEDIAN;
        } else if (operation == Filter.Operation.STDDEV) {
            return GeneralFilterBand.OpType.STDDEV;
        } else {
            throw new IllegalArgumentException("illegal operation: " + operation);
        }
    }

    private static Kernel getKernel(Filter filter) {
        return new Kernel(filter.getKernelWidth(),
                filter.getKernelHeight(),
                filter.getKernelOffsetX(),
                filter.getKernelOffsetY(),
                1.0 / filter.getKernelQuotient(),
                filter.getKernelElements());
    }

    static FilterBand getFilterBand(RasterDataNode sourceRaster, String bandName, Filter filter, int iterationCount) {
        FilterBand targetBand;
        Product targetProduct = sourceRaster.getProduct();

        if (filter.getOperation() == Filter.Operation.CONVOLVE) {
            targetBand = new ConvolutionFilterBand(bandName, sourceRaster, getKernel(filter), iterationCount);
            if (sourceRaster instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
            }
        } else {
            GeneralFilterBand.OpType opType = getOpType(filter.getOperation());
            targetBand = new GeneralFilterBand(bandName, sourceRaster, opType, getKernel(filter), iterationCount);
            if (sourceRaster instanceof Band) {
                ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
            }
        }

        targetBand.setDescription(String.format("Filter '%s' (=%s) applied to '%s'", filter.getName(), filter.getOperation(), sourceRaster.getName()));
        if (sourceRaster instanceof Band) {
            ProductUtils.copySpectralBandProperties((Band) sourceRaster, targetBand);
        }
        targetProduct.addBand(targetBand);
        ProductUtils.copyImageGeometry(sourceRaster, targetBand, false);
        targetBand.fireProductNodeDataChanged();
        return targetBand;
    }

    public FilterBand createdFilterBand(Filter filter, String bandName, int iterationCount){
        return null;
    }
}

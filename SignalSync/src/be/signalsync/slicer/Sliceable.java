package be.signalsync.slicer;

/**
 * Any implementing class has to be able to create a Slicer.
 * @author Ward Van Assche
 *
 * @param <T> The type of the slice result.
 * @param <S> The type of the returned Slicer object.
 */
public interface Sliceable<T, S extends Slicer<T>> {
	S createSlicer(int sliceStep, int sliceSize);
}

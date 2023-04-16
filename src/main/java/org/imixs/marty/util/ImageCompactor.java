package org.imixs.marty.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Stateless;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.imixs.workflow.ItemCollection;

/**
 * This ImageCompactor ejb provides a mechanism to resize new uploaded images
 * (.jpg).
 * <p>
 * image.maxWidth = maximal width of a image, indicates if a image should be
 * resized. image.fileExtension = indicates the file extension for images and
 * photos
 * 
 * 
 * @version 1.0
 * @author rsoika
 * 
 */
@Stateless
@LocalBean
public class ImageCompactor {

	private static Logger logger = Logger.getLogger(ImageCompactor.class.getName());

	String fileExtentions = "jpg,JPEG";

	/**
	 * This method tests if an attached is a photo and need to be resized
	 * 
	 * @param ctx
	 * @return
	 * @throws Exception
	 */

	public void resize(ItemCollection workitem, int maxSize) throws Exception {

		logger.finest("Image Interceptor started");
		List<String> filenames = workitem.getFileNames();
		for (String fileName : filenames) {
			if (isPhoto(fileName)) {
				BufferedImage originalImage = getImageFromWorkitem(workitem, fileName);

				// test if max with is extended?
				if (originalImage!=null && originalImage.getWidth() > maxSize) {
					logger.info("...rezise new photo: " + fileName);

					int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();

					// resize Image
					BufferedImage resizeImageHintJpg = resizeImageWithHint(originalImage, type, maxSize);

					if (resizeImageHintJpg != null) {

						// write image back...
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(resizeImageHintJpg, getFormatName(fileName), baos);
						baos.flush();
						byte[] imageInByte = baos.toByteArray();
						baos.close();

						// update workitem
						replaceImage(workitem, fileName, imageInByte);
					}
				}

			}
		}

	}

	/**
	 * Retruns true if name ends of a known extension. Default is 'jpg'
	 * 
	 * @param aname
	 * @return
	 */
	private boolean isPhoto(String aname) {
		if (aname == null)
			return false;

		StringTokenizer st = new StringTokenizer(fileExtentions, ",");
		while (st.hasMoreElements()) {
			String sExtention = st.nextToken().toLowerCase();
			if (aname.toLowerCase().endsWith(sExtention))
				return true;

		}
		return false;
	}

	private BufferedImage resizeImageWithHint(BufferedImage originalImage, int type, int imageMaxWidth) {

		// compute hight...
		float width = originalImage.getWidth();
		float height = originalImage.getHeight();
		float factor = (float) width / (float) imageMaxWidth;
		int newHeight = (int) (height / factor);

		BufferedImage resizedImage = new BufferedImage(imageMaxWidth, newHeight, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, imageMaxWidth, newHeight, null);
		g.dispose();
		g.setComposite(AlphaComposite.Src);

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		return resizedImage;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private BufferedImage getImageFromWorkitem(ItemCollection workItem, String file) {
		Map mapFiles = null;
		List vFiles = workItem.getItemValue("$file");
		if (vFiles != null && vFiles.size() > 0) {
			mapFiles = (Map) vFiles.get(0);

			List<Object> fileInfoList = new Vector<Object>();
			fileInfoList = (List<Object>) mapFiles.get(file);
			if (fileInfoList != null) {
				@SuppressWarnings("unused")
				String sContentType = fileInfoList.get(0).toString();
				byte[] fileContent = (byte[]) fileInfoList.get(1);
				if (fileContent != null && fileContent.length > 2) {
					try {

						Iterator<ImageReader> inReaders = ImageIO.getImageReadersByFormatName(getFormatName(file));

						ImageReader imageReader = (ImageReader) inReaders.next();
						ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(fileContent));

						imageReader.setInput(iis);
						BufferedImage originalImage = imageReader.read(0);
						return originalImage;

					} catch (IOException e) {
						logger.severe("ImageInerceptor - unable to load image from workitem : " + e.getMessage());
						e.printStackTrace();
					}
				}
			}
		}
		return null;

	}

	private String getFormatName(String aFilename) {
		if (aFilename.indexOf('.') == -1)
			return null;
		String inFormat = aFilename.substring(aFilename.lastIndexOf('.') + 1);

		return inFormat.toLowerCase();
	}

	/**
	 * This method replace the content of an image
	 * 
	 * @param workItem
	 * @param file
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void replaceImage(ItemCollection workItem, String file, byte[] content) {

		if (content == null)
			return;
		Map mapFiles = null;
		List vFiles = workItem.getItemValue("$file");
		if (vFiles != null && vFiles.size() > 0) {
			mapFiles = (Map) vFiles.get(0);

			List<Object> fileInfoList = new Vector<Object>();
			fileInfoList = (List<Object>) mapFiles.get(file);
			if (fileInfoList != null) {
				// replace new content...
				fileInfoList.set(1, content);
				mapFiles.put(file, fileInfoList);
			}
		}

		// update file property...
		Vector vNew = new Vector();
		vNew.add(mapFiles);
		workItem.replaceItemValue("$file", vNew);

	}

}

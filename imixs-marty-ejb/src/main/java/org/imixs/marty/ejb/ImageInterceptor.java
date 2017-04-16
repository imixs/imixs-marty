package org.imixs.marty.ejb;

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
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;

import javax.ejb.EJB;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.imixs.workflow.ItemCollection;
import org.imixs.workflow.engine.PropertyService;

/**
 * This Intercepter class provides a mechanism to resize new uploaded images
 * (.jpg).
 * The behavior can be configured through the imixs.property file. 
 * 
 * 
 * image.maxWidth = maximal width of a image, indicates if a image should be resized.
 * image.fileExtension = indicates the file extension for images and photos
 * 
 * 
 * @version 1.0
 * @author rsoika
 * 
 */

public class ImageInterceptor {

	private static Logger logger = Logger.getLogger(ImageInterceptor.class.getName());
	private int imageMaxWidth;
	
	@EJB
	PropertyService propertyService;

	

	/**
	 * The interceptor method tests if new files were uploaded.
	 * 
	 * The interceptor runns only in a save method
	 * 
	 * @param ctx
	 * @return
	 * @throws Exception
	 */
	@AroundInvoke
	public Object intercept(InvocationContext ctx) throws Exception {

		// test method name
		String sMethod = ctx.getMethod().getName();
		if ("save".equals(sMethod)) {
			// get workitem....
			Object[] params = ctx.getParameters();

			ItemCollection workitem = (ItemCollection) params[0];
			if (workitem != null
					&& "workitemlob"
							.equals(workitem.getItemValueString("Type"))) {

				Properties prop =  propertyService.getProperties();
				String sMaxWidth = prop.getProperty("image.maxWidth",
						"1024");
				try {
					imageMaxWidth = Integer.parseInt(sMaxWidth);
				} catch (NumberFormatException ne) {
					logger.warning("ImageInterceptor can not parse property 'image.maxwidth'! "
							+ ne.getMessage());
					imageMaxWidth = 1024;
				}

				logger.finest("Image Interceptor started");
				List<String> filenames = workitem.getFileNames();
				for (String fileName : filenames) {
					if (isPhoto(fileName)) {
						// photolist.add(filename);

						logger.fine("ImageInterceptor testing new photo: " + fileName);

						BufferedImage originalImage = getImageFromWorkitem(
								workitem, fileName);

						// test if max with is extended?
						if (originalImage.getWidth() > imageMaxWidth) {
							logger.info("ImageInterceptor rezise new photo: " + fileName);

							int type = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB
									: originalImage.getType();

							// resize Image
							BufferedImage resizeImageHintJpg = resizeImageWithHint(
									originalImage, type);

							if (resizeImageHintJpg != null) {
								
								
								
								// write image back...
								/*
								ImageIO.write(
										resizeImageHintJpg,
										getFormatName(fileName),
										new File(
												"/home/rsoika/Downloads/mkyong_hint_jpg.jpg"));
								*/
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								ImageIO.write( resizeImageHintJpg, getFormatName(fileName), baos );
								baos.flush();
								byte[] imageInByte = baos.toByteArray();
								baos.close();
								
								// update workitem 
								replaceImage(workitem, fileName,imageInByte) ;
							}
						}

					}
				}

			}
		}

		return ctx.proceed();
	}

	/**
	 * Retruns true if name ends of a known extension. Default is 'jpg'
	 * 
	 * @param aname
	 * @return
	 */
	public boolean isPhoto(String aname) {
		if (aname == null)
			return false;

		Properties prop =  propertyService.getProperties();
		
		String fileExtentions = prop.getProperty("image.fileExtension",
				"jpg,JPEG");
		StringTokenizer st = new StringTokenizer(fileExtentions, ",");
		while (st.hasMoreElements()) {
			String sExtention = st.nextToken().toLowerCase();
			if (aname.toLowerCase().endsWith(sExtention))
				return true;

		}
		return false;
	}

	private BufferedImage resizeImageWithHint(BufferedImage originalImage,
			int type) {

		// compute hight...
		float width = originalImage.getWidth();
		float height = originalImage.getHeight();
		float factor = (float) width / (float) imageMaxWidth;
		int newHeight = (int) (height / factor);

		BufferedImage resizedImage = new BufferedImage(imageMaxWidth,
				newHeight, type);
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage, 0, 0, imageMaxWidth, newHeight, null);
		g.dispose();
		g.setComposite(AlphaComposite.Src);

		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		return resizedImage;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private BufferedImage getImageFromWorkitem(ItemCollection workItem,
			String file) {
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

				try {
				
					Iterator<ImageReader> inReaders = ImageIO
							.getImageReadersByFormatName(getFormatName(file));

					ImageReader imageReader = (ImageReader) inReaders.next();
					ImageInputStream iis = ImageIO
							.createImageInputStream(new ByteArrayInputStream(
									fileContent));

					imageReader.setInput(iis);
					BufferedImage originalImage = imageReader.read(0);
					return originalImage;

				} catch (IOException e) {
					logger.severe("ImageInerceptor - unable to load image from workitem : "
							+ e.getMessage());
					e.printStackTrace();
				}
			}
		}
		return null;

	}

	
	private String getFormatName(String aFilename) {
		if (aFilename.indexOf('.')==-1)
			return null;
		String inFormat = aFilename.substring(aFilename.lastIndexOf('.') + 1);

		return inFormat.toLowerCase();
	}
	
	
	
	
	/**
	 * This method replace the content of an image
	 * @param workItem
	 * @param file
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void replaceImage(ItemCollection workItem,
			String file, byte[] content) {
		
		if (content==null)
			return;
		Map mapFiles = null;
		List vFiles =  workItem.getItemValue("$file");
		if ( vFiles != null && vFiles.size() > 0) {
			mapFiles = (Map) vFiles.get(0);

			List<Object> fileInfoList = new Vector<Object>();
			fileInfoList =  (List<Object>) mapFiles.get(file);
			if (fileInfoList != null) {
				// replace new content...
				fileInfoList.set(1, content);
				mapFiles.put(file, fileInfoList);
			}
		}
		
		// update file property...
		Vector vNew=new Vector();
		vNew.add(mapFiles);
		workItem.replaceItemValue("$file", vNew);
		

	}

	

}

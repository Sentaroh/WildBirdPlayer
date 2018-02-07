package com.sentaroh.android.WildBirdPlayer;

import static com.sentaroh.android.WildBirdPlayer.Constants.APPLICATION_TAG;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.Dialog.ProgressSpinDialogFragment;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.media.ExifInterface;
import android.util.Log;

@SuppressLint("DefaultLocale")
public class ArtWorkUtil {
	final static private void createCacheDir(GlobalParameters mGlblParms) {
		File lf=new File(getCacheDir(mGlblParms));
		lf.mkdirs();
	};
	
	final static public String getCacheDir(GlobalParameters mGlblParms) {
		String cache_file_dir=mGlblParms.wildBirdPlayerHomeDir+"/cache";
		return cache_file_dir;
	};

	final static private void houseKeepCacheFile(GlobalParameters mGlblParms,
			ArrayList<MusicFileListItem> masterFileList) {
		File lf=new File(getCacheDir(mGlblParms));
		File[] fl=lf.listFiles();
		if (fl==null || fl.length==0) return;
		for (int i=0;i<fl.length;i++) {
			String cache_path=fl[i].getPath();
			MusicFileListItem mfli=findMusicFileListItemByCacheFileName(mGlblParms,masterFileList,cache_path);
			if (mfli==null) {
				fl[i].delete();
			}
		}
		
	};

	final static private MusicFileListItem findMusicFileListItemByCacheFileName(GlobalParameters mGlblParms,
			ArrayList<MusicFileListItem> masterFileList, String cache_path) {
		MusicFileListItem mfli=null;
		int mflc=masterFileList.size();
		for(int i=0;i<mflc;i++) {
			mfli=masterFileList.get(i);
			String mfli_cache_path=getCacheDir(mGlblParms)+"/"+
					mfli.musicFolderName.replace("/", "_")+"_"+mfli.wk_m_name;
			if (mfli_cache_path.equals(cache_path)) {
				return mfli;
			}
		}
		return null;
	};
	
	final static public void UpdateImageFileInfo(GlobalParameters mGlblParms,
			ThreadCtrl tc,
			ProgressSpinDialogFragment pdf,
			ArrayList<MusicFileListItem> masterFileList,
			String path) {
		if (mGlblParms.debugEnabled) 
			Log.v(APPLICATION_TAG,"addImageFileInfoToMusicFileList Entered, path="+path);
		ArrayList<ImageFileListItem> imageFileList=new ArrayList<ImageFileListItem>();
		createImageFileList(mGlblParms, imageFileList, path);
		int begin_ptr=0;
		MusicFileListItem pfli=null;
		for (int i=0;i<masterFileList.size();i++) {
			MusicFileListItem fli=masterFileList.get(i);
			String ft="";
			int ft_pos=fli.musicFileName.lastIndexOf(".");
			ft=fli.musicFileName.substring(ft_pos+1);
			fli.wk_m_name=fli.musicFileName.replace("."+ft, "");
			fli.wk_image_path_prefix=fli.musicFilePath+"/"+fli.wk_m_name+"_";
			
			if (pfli!=null && pfli.wk_image_path_prefix.equals(fli.wk_image_path_prefix)) {
				fli.wk_image_begin_cnt=pfli.wk_image_begin_cnt;
//				Log.v("","fp="+fli.musicFilePath+"/"+fli.musicFileName);
			} else {
				for (int j=begin_ptr;j<imageFileList.size();j++) {
					ImageFileListItem awfli=imageFileList.get(j);
//					Log.v("","path="+awfli.file_path+", pref="+fli.aw_image_path_prefix);
					if (awfli.file_path.startsWith(fli.wk_image_path_prefix)) {
//						Log.v("","j="+j);
						fli.wk_image_begin_cnt=j;
						begin_ptr=j;
						pfli=fli;
						break;
					}
				}
			}
		}
		createCacheDir(mGlblParms);
		String cache_dir=getCacheDir(mGlblParms);

		long to_be_processed_count=masterFileList.size();
		boolean house_keep_required=false;
		String msg_txt=mGlblParms.appContext.getString(R.string.msgs_main_building_file_list_msg_image);
		for (int i=0;i<masterFileList.size();i++) {
			if (!tc.isEnabled()) {
				break;
			}
			long progress=((long)(i*100))/to_be_processed_count;
			MusicFileListItem fli=masterFileList.get(i);
			
			pdf.updateMsgText(
					String.format(msg_txt,progress, fli.musicFolderName+"/"+fli.wk_m_name));
//			String m_name=fli.musicFileName.replace("."+ft, "");
//			String image_path_prefix=fli.musicFilePath+"/"+m_name;
			String cache_path=cache_dir+"/"+
					fli.musicFolderName.replace("/", "_")+"_"+fli.wk_m_name;
			
			ArrayList<ImageFileListItem> s_fl=new ArrayList<ImageFileListItem>();
			for (int j=fli.wk_image_begin_cnt;j<imageFileList.size();j++) {
				ImageFileListItem awfli=imageFileList.get(j);
				if (awfli.file_path.startsWith(fli.wk_image_path_prefix)) {
					s_fl.add(awfli);
				} else {
					if (awfli.file_path.compareToIgnoreCase(fli.wk_image_path_prefix)>0) {
						break;
					}
					
				}
			}
//			String aa=null;
//			aa.length();
//			Log.v("","s_fl size="+s_fl.size());
			
//			ArrayList<ArtWorkImageListItem>awil=loadArtWorkImageList(mGlblParms, cache_path);
			if (s_fl.size()>0) {
				if (fli.artWorkImageList==null || 
						!isArtWorkImageFileExists(mGlblParms, cache_path)) {
					//File create required
					fli.artWorkImageList=new ArrayList<ImageFileListItem>();
					for(int k=0;k<s_fl.size();k++) {
						ImageFileListItem awili=new ImageFileListItem();
						awili.file_path=s_fl.get(k).file_path;
						awili.file_type=s_fl.get(k).file_type;
						awili.file_last_modified=s_fl.get(k).file_last_modified;
						awili.file_length=s_fl.get(k).file_length;
//						awili.artWorkByteArray=createArtWorkImageByteArray(mGlblParms, s_fl.get(k));
						fli.artWorkImageList.add(awili);
						fli.fileListItemUpdated=true;
					}
					createImageByteArrayFile(mGlblParms, cache_path, fli.artWorkImageList);
					house_keep_required=true;
				} else {
					//Check modified
					if (fli.artWorkImageList.size()!=s_fl.size()) {
						//no of file are diff
						fli.artWorkImageList=new ArrayList<ImageFileListItem>();
						for(int k=0;k<s_fl.size();k++) {
							ImageFileListItem awili=new ImageFileListItem();
							awili.file_path=s_fl.get(k).file_path;
							awili.file_type=s_fl.get(k).file_type;
							awili.file_last_modified=s_fl.get(k).file_last_modified;
							awili.file_length=s_fl.get(k).file_length;
//							awili.artWorkByteArray=createArtWorkImageByteArray(mGlblParms, s_fl.get(k));
							fli.artWorkImageList.add(awili);
							fli.fileListItemUpdated=true;
						}
						createImageByteArrayFile(mGlblParms, cache_path, fli.artWorkImageList);
						house_keep_required=true;
					} else {
						boolean save_required=false;
						for (int idx_diff=0;idx_diff<fli.artWorkImageList.size();idx_diff++) {
							ImageFileListItem awili=fli.artWorkImageList.get(idx_diff);
							boolean diff=false;
							if (!awili.file_path.equals(s_fl.get(idx_diff).file_path)) {
								//File name diff
//								Log.v("","name diff");
								diff=true;
							} else if (awili.file_last_modified!=s_fl.get(idx_diff).file_last_modified) {
								//last mod diff
//								Log.v("","last mod diff");
								diff=true;
							} else if (awili.file_length!=s_fl.get(idx_diff).file_length) {
								//Size diff
//								Log.v("","length diff");
								diff=true;
							}
							if (diff) {
								save_required=true;
								awili.file_path=s_fl.get(idx_diff).file_path;
								awili.file_type=s_fl.get(idx_diff).file_type;
								awili.file_last_modified=s_fl.get(idx_diff).file_last_modified;
								awili.file_length=s_fl.get(idx_diff).file_length;
//								awili.artWorkByteArray=
//										createArtWorkImageByteArray(mGlblParms, s_fl.get(idx_diff));
								fli.fileListItemUpdated=true;
							}
						}
						if (save_required) {
							createImageByteArrayFile(mGlblParms, cache_path, fli.artWorkImageList);
							house_keep_required=true;
						}
					}
				}
			} else {
				if (fli.artWorkImageList==null) {
					//Nothing
				} else {
					//File delete required
					deleteImageByteArrayFile(mGlblParms,cache_path);
					house_keep_required=true;
				}
			}
		}
		
		if (house_keep_required) houseKeepCacheFile(mGlblParms, masterFileList);
	};

	final static private byte[] createImageByteArrayWithResize(GlobalParameters mGlblParms,
			String fp) {
		byte[] bm_result=null;
		File lf=new File(fp);
		try {
			FileInputStream fis = new FileInputStream(lf);
			BufferedInputStream bis=new BufferedInputStream(fis,1024*2048);
			byte[] bm_file=new byte[(int) lf.length()];
			bis.read(bm_file);
			bis.close();
			
			BitmapFactory.Options imageOptions = new BitmapFactory.Options();
			imageOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(bm_file,0,bm_file.length, imageOptions);

			float imageScaleWidth = (float)imageOptions.outWidth / mGlblParms.settingImagesizeMax;
			float imageScaleHeight = (float)imageOptions.outHeight / mGlblParms.settingImagesizeMax;

			if (imageScaleWidth > 2 && imageScaleHeight > 2) {  
			    BitmapFactory.Options imageOptions2 = new BitmapFactory.Options();

			    int imageScale = 
			    		(int)Math.floor((imageScaleWidth > imageScaleHeight ? imageScaleHeight : imageScaleWidth));    

			    for (int i = 2; i < imageScale; i *= 2) {
			        imageOptions2.inSampleSize = i;
			    }
			    if (imageOptions2.inSampleSize>0) {
				    Bitmap bitmap = BitmapFactory.decodeByteArray(bm_file,0,bm_file.length, imageOptions2);
//				    Log.v("image", "Sample Size: 1/" + imageOptions2.inSampleSize);
					ByteArrayOutputStream bos=new ByteArrayOutputStream();
					bitmap.compress(CompressFormat.JPEG, mGlblParms.settingImageQuality, bos);
					bos.flush();
					bos.close();
					bitmap.recycle();
					bitmap=null;
					bm_result=bos.toByteArray();
					if (mGlblParms.debugEnabled) 
						Log.v(APPLICATION_TAG,"Image file="+fp+
								", Original Image Size: " + imageOptions.outWidth +
								" x " + imageOptions.outHeight+
								", Scale factor="+imageOptions2.inSampleSize);
			    } else {
			    	bm_result=bm_file;
			    }
			} else {
			    //bitmap = BitmapFactory.decodeByteArray(bm,0,bm.length);
				bm_result=bm_file;
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bm_result;
	}

	final static private byte[] createImageByteArrayNoResize(GlobalParameters mGlblParms,
			String fp) {
		byte[] bm_result=null;
		File lf=new File(fp);
		try {
			FileInputStream fis = new FileInputStream(lf);
			BufferedInputStream bis=new BufferedInputStream(fis,1024*2048);
			byte[] bm_file=new byte[(int) lf.length()];
			bis.read(bm_file);
			bis.close();
			bm_result=bm_file;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return bm_result;
	}

	@SuppressWarnings("deprecation")
	final static public void listExifInfo(String fp, ImageByteArrayListItem ifli) {
		ExifInterface ei;
		try {
			ei = new ExifInterface(fp);
			Metadata metaData=ImageMetadataReader.readMetadata(new File(fp));
			if (metaData!=null) {//ei!=null) {
				ifli.exif_image_length=ei.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, -1);
				ifli.exif_image_width=ei.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, -1);
				
				// EXIFのディレクトリ(情報種類)を読み込み
				ExifIFD0Directory ifdDirectory = metaData.getFirstDirectoryOfType(ExifIFD0Directory.class);
				ExifSubIFDDirectory directory = metaData.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
				if (directory!=null) {
					// タグの値を読み込み
					Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
					ifli.exif_image_aperture=directory.getString(ExifSubIFDDirectory.TAG_FNUMBER)==null?"":directory.getString(ExifSubIFDDirectory.TAG_FNUMBER);
					ifli.exif_image_date_time=date==null?"":date.toLocaleString();
					ifli.exif_image_exposure=directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME)==null?"":directory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
					ifli.exif_image_focal_length=directory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH)==null?"":directory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
					ifli.exif_image_iso=directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT)==null?"":directory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
					if (ifdDirectory!=null) {
						Log.v("","h="+ifdDirectory.getString(ExifIFD0Directory.TAG_IMAGE_HEIGHT));
						Log.v("","w="+ifdDirectory.getString(ExifIFD0Directory.TAG_IMAGE_WIDTH));
						ifli.exif_image_make=ifdDirectory.getString(ExifIFD0Directory.TAG_MAKE)==null?"":ifdDirectory.getString(ExifIFD0Directory.TAG_MAKE);
						ifli.exif_image_model=ifdDirectory.getString(ExifIFD0Directory.TAG_MODEL)==null?"":ifdDirectory.getString(ExifIFD0Directory.TAG_MODEL);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ImageProcessingException e) {
			e.printStackTrace();
		}
	}
	
	final static private void createImageByteArrayFile(
			GlobalParameters mGlblParms, String cache_path,
			ArrayList<ImageFileListItem> awil) {
		try {
			ArrayList<ImageByteArrayListItem> iflist=new ArrayList<ImageByteArrayListItem>();
			for(int k=0;k<awil.size();k++) {
				ImageByteArrayListItem awbali=new ImageByteArrayListItem();
				awbali.file_type=awil.get(k).file_type;
//				Log.v("","ct="+awil.get(k).file_type);
				if (awil.get(k).file_type.equals("gif")) {
					awbali.imageByteArray=
							createImageByteArrayNoResize(mGlblParms, awil.get(k).file_path);
				} else {
					awbali.imageByteArray=
							createImageByteArrayWithResize(mGlblParms, awil.get(k).file_path);
				}
				listExifInfo(awil.get(k).file_path, awbali);
				iflist.add(awbali);
			}

			FileOutputStream fos=new FileOutputStream(cache_path);
			BufferedOutputStream bos=new BufferedOutputStream(fos,1024*2048);
			ObjectOutputStream oos=new ObjectOutputStream(bos);
			oos.writeInt(iflist.size());
			for(int i=0;i<iflist.size();i++) {
				iflist.get(i).writeExternal(oos);
			}
			oos.flush();
			oos.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

	final static public boolean isArtWorkImageFileExists(
			GlobalParameters mGlblParms, String cache_path) {
		File lf=new File(cache_path);
		return lf.exists();
	};
	
	final static public ArrayList<ImageByteArrayListItem> loadArtWorkImageByteArrayFile(
			GlobalParameters mGlblParms, String cache_path) {
		ArrayList<ImageByteArrayListItem> awbal=new ArrayList<ImageByteArrayListItem>();
		try {
			FileInputStream fis=new FileInputStream(cache_path);
			BufferedInputStream bis=new BufferedInputStream(fis,1024*2048);
			ObjectInputStream ois=new ObjectInputStream(bis);
			int bm_cnt=ois.readInt();
			for(int i=0;i<bm_cnt;i++) {
				ImageByteArrayListItem awbali=new ImageByteArrayListItem();
				awbali.readExternal(ois);
				awbal.add(awbali);
			}
			ois.close();
			bis.close();
			fis.close();
		} catch (FileNotFoundException e) {
//			e.printStackTrace();
			awbal=null;
		} catch (StreamCorruptedException e) {
			e.printStackTrace();
			awbal=null;
		} catch (IOException e) {
			e.printStackTrace();
			awbal=null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			awbal=null;
		}
		return awbal;
	};
	
	final static public void deleteImageByteArrayFile(
			GlobalParameters mGlblParms, String cache_path) {
		File lf=new File(cache_path);
		lf.delete();
		return ;
	};

	final static public void deleteAllImageByteArray(
			GlobalParameters mGlblParms) {
		File lf=new File(getCacheDir(mGlblParms));
		File[] d_fl=lf.listFiles();
		if (d_fl!=null && d_fl.length>0) {
			for(int i=0;i<d_fl.length;i++) {
				d_fl[i].delete();
			}
		}
		return ;
	};

	final static public void createImageFileList(GlobalParameters mGlblParms,
			ArrayList<ImageFileListItem> imageFileList, String path) {
		File lf=new File(path);
		if (lf.exists()) {
			if (lf.isDirectory()) {
				File[] fa=lf.listFiles();
				if (fa!=null) {
					for (int i=0;i<fa.length;i++) {
//						Log.v("","i="+i+", path="+fa[i].getPath()+", dir="+fa[i].isDirectory());
						if (fa[i].isDirectory()) {
							createImageFileList(mGlblParms,imageFileList,
									(path+"/"+fa[i].getName()).replaceAll("//", "/"));
						} else {
							String fileName=fa[i].getName();
							String ft="";
							int ft_pos=fileName.lastIndexOf(".");
							ft=fileName.substring(ft_pos+1).toLowerCase();
							if (ft.equals("jpg")) {
								ImageFileListItem awfli=new ImageFileListItem();
								awfli.file_path=fa[i].getPath();
								awfli.file_type=ft;
								awfli.file_last_modified=fa[i].lastModified();
								awfli.file_length=fa[i].length();
								imageFileList.add(awfli);
//								Log.v("","awfli name="+awfli.file_path+", type="+ft);
							}
						}
					}
				} else {
					//Nothing
				}
			} else {
				String fileName=lf.getName();
				String ft="";
				int ft_pos=fileName.lastIndexOf(".");
				ft=fileName.substring(ft_pos+1);
				if (ft.equals("jpg")) {
					ImageFileListItem awfli=new ImageFileListItem();
					awfli.file_path=lf.getPath();
					awfli.file_type=ft;
					awfli.file_last_modified=lf.lastModified();
					awfli.file_length=lf.length();
					imageFileList.add(awfli);
//					Log.v("","awfli name="+awfli.file_path+", type="+ft);
				}
			}
		} else {
			//Nothing
		}
		Collections.sort(imageFileList, new Comparator<ImageFileListItem>(){
			@Override
			public int compare(ImageFileListItem lhs,
					ImageFileListItem rhs) {
				return lhs.file_path.compareToIgnoreCase(rhs.file_path);
			}
		});
	};

//	final static public void extractArtWorkImageFile(String path) {
//		File lf=new File(path);
//		if (lf.exists()) {
//			if (lf.isDirectory()) {
//				File[] fa=lf.listFiles();
//				if (fa!=null) {
//					for (int i=0;i<fa.length;i++) {
//						if (fa[i].isDirectory()) {
//							extractArtWorkImageFile((path+"/"+fa[i].getName()).replaceAll("//", "/"));
//						} else {
//							String fileName=fa[i].getName();
//							String ft="";
//							int ft_pos=fileName.lastIndexOf(".");
//							ft=fileName.substring(ft_pos+1);
//							if (ft.equals("m4a")) {
//								saveArtWorkImageFile(fa[i].getPath(), fa[i].getPath().replace("."+ft, ""));
//							}
//						}
//					}
//				} else {
//					//Nothing
//				}
//			} else {
//				String fileName=lf.getName();
//				String ft="";
//				int ft_pos=fileName.lastIndexOf(".");
//				ft=fileName.substring(ft_pos+1);
//				if (ft.equals("m4a")) {
//					saveArtWorkImageFile(lf.getPath(), lf.getPath().replace("."+ft, ""));
//				}
//			}
//		} else {
//			//Nothing
//		}
//	};
//	
//	final static public void saveArtWorkImageFile(String fp, String base_path) {
//		AudioFile f_current = null;
//		try {
//			f_current = AudioFileIO.read(new File(fp));
//		} catch (CannotReadException e) {
//			e.printStackTrace();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (TagException e) {
//			e.printStackTrace();
//		} catch (ReadOnlyFileException e) {
//			e.printStackTrace();
//		} catch (InvalidAudioFrameException e) {
//			e.printStackTrace(); 
//		}
//		if (f_current!=null) {
//			Tag tag_current = f_current.getTag();
////			AudioHeader ah= f.getAudioHeader();
////			Log.v("","tl="+f.getAudioHeader().getTrackLength());
////			Log.v("","ARTIST="+tag.getFirst(FieldKey.ARTIST));
////			Log.v("","ALBUM="+tag.getFirst(FieldKey.ALBUM));
////			Log.v("","TITLE="+tag.getFirst(FieldKey.TITLE));
////			Log.v("","COMMENT="+tag.getFirst(FieldKey.COMMENT));
////			Log.v("","YEAR="+tag.getFirst(FieldKey.YEAR));
////			Log.v("","TRACK="+tag.getFirst(FieldKey.TRACK));
////			Log.v("","DISC_NO="+tag.getFirst(FieldKey.DISC_NO));
////			Log.v("","COMPOSER="+tag.getFirst(FieldKey.COMPOSER));
////			Log.v("","ARTIST_SORT="+tag.getFirst(FieldKey.ARTIST_SORT));
//			
//			List<Artwork> t_list=tag_current.getArtworkList();
//			if (t_list!=null) {
//				for (int i=0;i<t_list.size();i++) {
//					FileOutputStream fos;
//					try {
//						fos = new FileOutputStream(base_path+"_"+i+".jpg");
//						fos.write(t_list.get(i).getBinaryData());
//						fos.flush();
//						fos.close();
//					} catch (FileNotFoundException e) {
//						e.printStackTrace();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//			
//		} else {
//		}
//
//	}

}

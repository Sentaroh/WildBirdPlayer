package com.sentaroh.android.WildBirdPlayer;

import static com.sentaroh.android.WildBirdPlayer.Constants.*;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.ArrayList;

public class FileListCache implements Serializable{
	private static final long serialVersionUID = SERIALIZABLE_VERSION_CODE;
	public ArrayList<MusicFileListItem> fileList=new ArrayList<MusicFileListItem>();
	public ArrayList<FolderListItem> folderList=new ArrayList<FolderListItem>();
}

class ImageFileListItem implements Externalizable{
	private static final long serialVersionUID = SERIALIZABLE_VERSION_CODE;
	public String file_path="";
	public String file_type="";
	public long file_last_modified=0L;
	public long file_length=0L;

	ImageFileListItem(){};
	
	@Override
	final public void readExternal(ObjectInput objin) throws IOException,
			ClassNotFoundException {
		long sid=objin.readLong();
		if (sid!=serialVersionUID) 
			throw new IOException("serialVersionUID was not matched by saved UID");
		file_path=objin.readUTF();
		file_last_modified=objin.readLong();
		file_length=objin.readLong();
		
	}
	@Override
	final public void writeExternal(ObjectOutput objout) throws IOException {
		objout.writeLong(serialVersionUID);
		objout.writeUTF(file_path);
		objout.writeLong(file_last_modified);
		objout.writeLong(file_length);
		
	}
}

class ImageByteArrayListItem implements Externalizable{
	private static final long serialVersionUID = SERIALIZABLE_VERSION_CODE;
	public String file_type="";
	public byte[] imageByteArray=null;
	
	public int exif_image_length=-1, exif_image_width=-1;
	public String exif_image_aperture="";
	public String exif_image_date_time="";
	public String exif_image_exposure="";
	public String exif_image_focal_length="";
	public String exif_image_iso="";
	public String exif_image_make="";
	public String exif_image_model="";

	ImageByteArrayListItem(){};
	
	@Override
	final public void readExternal(ObjectInput objin) throws IOException,
			ClassNotFoundException {
		long sid=objin.readLong();
		if (sid!=serialVersionUID) 
			throw new IOException("serialVersionUID was not matched by saved UID");
		file_type=objin.readUTF();
		imageByteArray=readArrayByte(objin);

		exif_image_length=objin.readInt();
		exif_image_width=objin.readInt();
		exif_image_aperture=objin.readUTF();
		exif_image_date_time=objin.readUTF();
		exif_image_exposure=objin.readUTF();
		exif_image_focal_length=objin.readUTF();
		exif_image_iso=objin.readUTF();
		exif_image_make=objin.readUTF();
		exif_image_model=objin.readUTF();

	}
	@Override
	final public void writeExternal(ObjectOutput objout) throws IOException {
		objout.writeLong(serialVersionUID);
		objout.writeUTF(file_type);
		writeArrayByte(objout, imageByteArray);
		
		objout.writeInt(exif_image_length);
		objout.writeInt(exif_image_width);
		objout.writeUTF(exif_image_aperture);
		objout.writeUTF(exif_image_date_time);
		objout.writeUTF(exif_image_exposure);
		objout.writeUTF(exif_image_focal_length);
		objout.writeUTF(exif_image_iso);
		objout.writeUTF(exif_image_make);
		objout.writeUTF(exif_image_model);

	}
	
	
	final static public byte[] readArrayByte(ObjectInput input) throws IOException{
		int lsz=input.readInt();
		byte[] result=null;
		if (lsz!=-1) {
			result=new byte[lsz];
			if (lsz>0) input.readFully(result,0,lsz);
		}
		return result;
	};
	
	final static public void writeArrayByte(ObjectOutput output, byte[] al) throws IOException {
		int lsz=-1;
		if (al!=null) {
			if (al.length!=0) lsz=al.length;
			else lsz=0;
		}
		output.writeInt(lsz);
		if (lsz>0) {
			output.write(al,0,lsz);
		}
	};

}

class MusicFileListItem implements Externalizable{
	private static final long serialVersionUID = SERIALIZABLE_VERSION_CODE;
	public String musicFilePath=null;
	public String musicFolderName=null;
	public String musicFileName=null;
	public long musicFileLastModified=0;
	public long musicFileSize=0;
	public int musicFileTrackLength=0;
	public long descriptionFileLastModified=0;
	public long descriptionFileSize=0;
	public String descriptionFileName=null;
	public String descriptionString=null;
	public boolean descriptionIsAvailable=false;
	
	public boolean fileListItemUpdated=false;
	public boolean fileListItemRefered=false;
	
	public int wk_image_begin_cnt=0;
	public String wk_m_name="", wk_image_path_prefix="";
	
	public ArrayList<ImageFileListItem> artWorkImageList=null;
	
//	public Bitmap thumbnaiBitmap=null;
	public MusicFileListItem() {};
	@Override
	final public void readExternal(ObjectInput objin) throws IOException,
			ClassNotFoundException {
		if (objin.readLong()!=serialVersionUID) 
			throw new IOException("serialVersionUID was not matched by saved UID");
		musicFilePath=objin.readUTF();
		musicFolderName=objin.readUTF();
		musicFileName=objin.readUTF();
		musicFileLastModified=objin.readLong();
		musicFileSize=objin.readLong();
		musicFileTrackLength=objin.readInt();
		descriptionFileLastModified=objin.readLong();
		descriptionFileSize=objin.readLong();
		
		if (objin.readByte()!=0) descriptionFileName=objin.readUTF();
		
		descriptionIsAvailable=objin.readBoolean();
		
		if (objin.readByte()!=0) descriptionString=objin.readUTF();

		int bm_cnt=objin.readInt();
		if (bm_cnt!=-1) {
			artWorkImageList=new ArrayList<ImageFileListItem>();
			for(int i=0;i<bm_cnt;i++) {
				ImageFileListItem awli=new ImageFileListItem();
				awli.readExternal(objin);
				artWorkImageList.add(awli);
			}
		}
	}
	@Override
	final public void writeExternal(ObjectOutput objout) throws IOException {
		objout.writeLong(serialVersionUID);
		objout.writeUTF(musicFilePath);
		objout.writeUTF(musicFolderName);
		objout.writeUTF(musicFileName);
		objout.writeLong(musicFileLastModified);
		objout.writeLong(musicFileSize);
		objout.writeInt(musicFileTrackLength);
		objout.writeLong(descriptionFileLastModified);
		objout.writeLong(descriptionFileSize);
		
		if (descriptionFileName==null) {
			objout.writeByte(0);
		}else {
			objout.writeByte(1);
			objout.writeUTF(descriptionFileName);
		}
		
		objout.writeBoolean(descriptionIsAvailable);
		
		if (descriptionString==null) {
			objout.writeByte(0);
		}else {
			objout.writeByte(1);
			objout.writeUTF(descriptionString);
		}
		
		if (artWorkImageList==null) {
			objout.writeInt(-1);
		} else {
			objout.writeInt(artWorkImageList.size());
			for(int i=0;i<artWorkImageList.size();i++) {
				artWorkImageList.get(i).writeExternal(objout);
			}
		}
	}
}

class FolderListItem implements Serializable{
	private static final long serialVersionUID = SERIALIZABLE_VERSION_CODE;
	public boolean selected=false;
	public String folderName=null;
//	public int noberOfFiles=0;
}
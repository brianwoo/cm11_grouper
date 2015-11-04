#ifndef _RAR_FILEFN_
#define _RAR_FILEFN_

enum MKDIR_CODE {MKDIR_SUCCESS,MKDIR_ERROR,MKDIR_BADPATH};

#ifndef __BIONIC__
MKDIR_CODE MakeDir(const char *Name,const wchar *NameW,bool SetAttr,uint Attr);
#else
MKDIR_CODE MakeDir(const char *Name,bool SetAttr,uint Attr);
#endif
bool CreatePath(const char *Path,bool SkipLastName);
#ifndef __BIONIC__
bool CreatePath(const wchar *Path,bool SkipLastName);
bool CreatePath(const char *Path,const wchar *PathW,bool SkipLastName);
void SetDirTime(const char *Name,const wchar *NameW,RarTime *ftm,RarTime *ftc,RarTime *fta);
#else
void SetDirTime(const char *Name,RarTime *ftm,RarTime *ftc,RarTime *fta);
#endif
bool IsRemovable(const char *Name);

#ifndef SFX_MODULE
int64 GetFreeDisk(const char *Name);
#endif

#ifndef __BIONIC__
bool FileExist(const char *Name,const wchar *NameW=NULL);
bool FileExist(const wchar *Name);
bool WildFileExist(const char *Name,const wchar *NameW=NULL);
#else
bool FileExist(const char *Name);
bool WildFileExist(const char *Name);
#endif
bool IsDir(uint Attr);
bool IsUnreadable(uint Attr);
bool IsLabel(uint Attr);
bool IsLink(uint Attr);
void SetSFXMode(const char *FileName);
void EraseDiskContents(const char *FileName);
bool IsDeleteAllowed(uint FileAttr);
#ifndef __BIONIC__
void PrepareToDelete(const char *Name,const wchar *NameW=NULL);
uint GetFileAttr(const char *Name,const wchar *NameW=NULL);
bool SetFileAttr(const char *Name,const wchar *NameW,uint Attr);
#else
void PrepareToDelete(const char *Name);
uint GetFileAttr(const char *Name);
bool SetFileAttr(const char *Name,uint Attr);
#endif

enum CALCCRC_SHOWMODE {CALCCRC_SHOWNONE,CALCCRC_SHOWTEXT,CALCCRC_SHOWALL};
uint CalcFileCRC(File *SrcFile,int64 Size=INT64NDF,CALCCRC_SHOWMODE ShowMode=CALCCRC_SHOWNONE);

#ifndef __BIONIC__
bool RenameFile(const char *SrcName,const wchar *SrcNameW,const char *DestName,const wchar *DestNameW);
#else
bool RenameFile(const char *SrcName,const char *DestName);
#endif
bool DelFile(const char *Name);
#ifndef __BIONIC__
bool DelFile(const char *Name,const wchar *NameW);
#else
bool DelFile(const char *Name);
#endif
bool DelDir(const char *Name);
#ifndef __BIONIC__
bool DelDir(const char *Name,const wchar *NameW);
#else
bool DelDir(const char *Name);
#endif

#if defined(_WIN_ALL) && !defined(_WIN_CE)
bool SetFileCompression(char *Name,wchar *NameW,bool State);
#endif




#endif

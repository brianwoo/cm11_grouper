#ifndef _RAR_FILECREATE_
#define _RAR_FILECREATE_

#ifndef __BIONIC__
bool FileCreate(RAROptions *Cmd,File *NewFile,char *Name,wchar *NameW,
                OVERWRITE_MODE Mode,bool *UserReject,int64 FileSize=INT64NDF,
                uint FileTime=0,bool WriteOnly=false);
bool GetAutoRenamedName(char *Name,wchar *NameW);
#else
bool FileCreate(RAROptions *Cmd,File *NewFile,char *Name,
                OVERWRITE_MODE Mode,bool *UserReject,int64 FileSize=INT64NDF,
                uint FileTime=0,bool WriteOnly=false);
bool GetAutoRenamedName(char *Name);
#endif

#if defined(_WIN_ALL) && !defined(_WIN_CE)
bool UpdateExistingShortName(wchar *Name);
#endif

#endif

#ifndef _RAR_PATHFN_
#define _RAR_PATHFN_

char* PointToName(const char *Path);
#ifndef __BIONIC__
wchar* PointToName(const wchar *Path);
#endif
char* PointToLastChar(const char *Path);
#ifndef __BIONIC__
wchar* PointToLastChar(const wchar *Path);
#endif
char* ConvertPath(const char *SrcPath,char *DestPath);
#ifndef __BIONIC__
wchar* ConvertPath(const wchar *SrcPath,wchar *DestPath);
#endif
void SetExt(char *Name,const char *NewExt);
#ifndef __BIONIC__
void SetExt(wchar *Name,const wchar *NewExt);
#endif
void SetSFXExt(char *SFXName);
#ifndef __BIONIC__
void SetSFXExt(wchar *SFXName);
#endif
char *GetExt(const char *Name);
#ifndef __BIONIC__
wchar *GetExt(const wchar *Name);
#endif
bool CmpExt(const char *Name,const char *Ext);
#ifndef __BIONIC__
bool CmpExt(const wchar *Name,const wchar *Ext);
#endif
#ifndef __BIONIC__
bool IsWildcard(const char *Str,const wchar *StrW=NULL);
#else
bool IsWildcard(const char *Str);
#endif
bool IsPathDiv(int Ch);
bool IsDriveDiv(int Ch);
int GetPathDisk(const char *Path);
#ifndef __BIONIC__
int GetPathDisk(const wchar *Path);
#endif
void AddEndSlash(char *Path);
#ifndef __BIONIC__
void AddEndSlash(wchar *Path);
#endif
void GetFilePath(const char *FullName,char *Path,int MaxLength);
#ifndef __BIONIC__
void GetFilePath(const wchar *FullName,wchar *Path,int MaxLength);
#endif
void RemoveNameFromPath(char *Path);
#ifndef __BIONIC__
void RemoveNameFromPath(wchar *Path);
#endif
void GetAppDataPath(char *Path);
#ifndef __BIONIC__
void GetAppDataPath(wchar *Path);
#endif
void GetRarDataPath(char *Path);
#ifndef __BIONIC__
void GetRarDataPath(wchar *Path);
bool EnumConfigPaths(wchar *Path,int Number);
#endif
bool EnumConfigPaths(char *Path,int Number);
void GetConfigName(const char *Name,char *FullName,bool CheckExist);
#ifndef __BIONIC__
void GetConfigName(const wchar *Name,wchar *FullName,bool CheckExist);
#endif
char* GetVolNumPart(char *ArcName);
#ifndef __BIONIC__
wchar* GetVolNumPart(wchar *ArcName);
void NextVolumeName(char *ArcName,wchar *ArcNameW,uint MaxLength,bool OldNumbering);
#else
void NextVolumeName(char *ArcName,uint MaxLength,bool OldNumbering);
#endif
bool IsNameUsable(const char *Name);
#ifndef __BIONIC__
bool IsNameUsable(const wchar *Name);
#endif
void MakeNameUsable(char *Name,bool Extended);
#ifndef __BIONIC__
void MakeNameUsable(wchar *Name,bool Extended);
#endif
char* UnixSlashToDos(char *SrcName,char *DestName=NULL,uint MaxLength=NM);
char* DosSlashToUnix(char *SrcName,char *DestName=NULL,uint MaxLength=NM);
#ifndef __BIONIC__
wchar* UnixSlashToDos(wchar *SrcName,wchar *DestName=NULL,uint MaxLength=NM);
wchar* DosSlashToUnix(wchar *SrcName,wchar *DestName=NULL,uint MaxLength=NM);
#endif
void ConvertNameToFull(const char *Src,char *Dest);
#ifndef __BIONIC__
void ConvertNameToFull(const wchar *Src,wchar *Dest);
#endif
bool IsFullPath(const char *Path);
#ifndef __BIONIC__
bool IsFullPath(const wchar *Path);
#endif
bool IsDiskLetter(const char *Path);
#ifndef __BIONIC__
bool IsDiskLetter(const wchar *Path);
#endif
void GetPathRoot(const char *Path,char *Root);
#ifndef __BIONIC__
void GetPathRoot(const wchar *Path,wchar *Root);
int ParseVersionFileName(char *Name,wchar *NameW,bool Truncate);
#else
int ParseVersionFileName(char *Name,bool Truncate);
#endif
char* VolNameToFirstName(const char *VolName,char *FirstName,bool NewNumbering);
#ifndef __BIONIC__
wchar* VolNameToFirstName(const wchar *VolName,wchar *FirstName,bool NewNumbering);
wchar* GetWideName(const char *Name,const wchar *NameW,wchar *DestW,size_t DestSize);
char* GetAsciiName(const wchar *NameW,char *Name,size_t DestSize);
#endif

#ifndef SFX_MODULE
#ifndef __BIONIC__
void GenerateArchiveName(char *ArcName,wchar *ArcNameW,size_t MaxSize,char *GenerateMask,bool Archiving);
#else
void GenerateArchiveName(char *ArcName,size_t MaxSize,char *GenerateMask,bool Archiving);
#endif
#endif

#endif

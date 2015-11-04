#ifndef _RAR_FINDDATA_
#define _RAR_FINDDATA_

enum FINDDATA_FLAGS {
  FDDF_SECONDDIR=1  // Second encounter of same directory in SCAN_GETDIRSTWICE ScanTree mode.
};

struct FindData
{
  char Name[NM];
#ifndef __BIONIC__
  wchar NameW[NM];
#endif
  int64 Size;
  uint FileAttr;
  uint FileTime;
  bool IsDir;
  RarTime mtime;
  RarTime ctime;
  RarTime atime;
#ifdef _WIN_ALL
  wchar ShortName[NM];
  FILETIME ftCreationTime; 
  FILETIME ftLastAccessTime; 
  FILETIME ftLastWriteTime; 
#endif
  uint Flags;
  bool Error;
};

class FindFile
{
  private:
#ifdef _WIN_ALL
    static HANDLE Win32Find(HANDLE hFind,const char *Mask,const wchar *MaskW,struct FindData *fd);
#endif

    char FindMask[NM];
#ifndef __BIONIC__
    wchar FindMaskW[NM];
#endif
    bool FirstCall;
#ifdef _WIN_ALL
    HANDLE hFind;
#else
    DIR *dirp;
#endif
  public:
    FindFile();
    ~FindFile();
    void SetMask(const char *FindMask);
#ifndef __BIONIC__
    void SetMaskW(const wchar *FindMaskW);
#endif
    bool Next(FindData *fd,bool GetSymLink=false);
#ifndef __BIONIC__
    static bool FastFind(const char *FindMask,const wchar *FindMaskW,FindData *fd,bool GetSymLink=false);
#else
    static bool FastFind(const char *FindMask,FindData *fd,bool GetSymLink=false);
#endif
};

#endif

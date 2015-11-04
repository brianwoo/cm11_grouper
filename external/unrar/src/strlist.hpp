#ifndef _RAR_STRLIST_
#define _RAR_STRLIST_

class StringList
{
  private:
    Array<char> StringData;
    size_t CurPos;

#ifndef __BIONIC__
    Array<wchar> StringDataW;
    size_t CurPosW;
#endif

    uint StringsCount;

#ifndef __BIONIC__
    size_t SaveCurPos[16],SaveCurPosW[16],SavePosNumber;
#else
    size_t SaveCurPos[16],SavePosNumber;
#endif
  public:
    StringList();
    void Reset();
    void AddString(const char *Str);
#ifndef __BIONIC__
    void AddString(const wchar *Str);
    void AddString(const char *Str,const wchar *StrW);
#endif
    bool GetString(char *Str,size_t MaxLength);
#ifndef __BIONIC__
    bool GetString(wchar *Str,size_t MaxLength);
    bool GetString(char *Str,wchar *StrW,size_t MaxLength);
    bool GetString(char *Str,wchar *StrW,size_t MaxLength,int StringNum);
#else
    bool GetString(char *Str,size_t MaxLength,int StringNum);
#endif
    char* GetString();
#ifndef __BIONIC__
    wchar* GetStringW();
    bool GetString(char **Str,wchar **StrW);
#else
    bool GetString(char **Str);
#endif
    void Rewind();
    uint ItemsCount() {return(StringsCount);};
    size_t GetCharCount();
#ifndef __BIONIC__
    bool Search(char *Str,wchar *StrW,bool CaseSensitive);
#else
    bool Search(char *Str,bool CaseSensitive);
#endif
    void SavePosition();
    void RestorePosition();
};

#endif

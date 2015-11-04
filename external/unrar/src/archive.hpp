#ifndef _RAR_ARCHIVE_
#define _RAR_ARCHIVE_

class Pack;

enum {EN_LOCK=1,EN_VOL=2,EN_FIRSTVOL=4};

enum ARCSIGN_TYPE {ARCSIGN_NONE,ARCSIGN_OLD,ARCSIGN_CURRENT,ARCSIGN_FUTURE};

class Archive:public File
{
  private:
    ARCSIGN_TYPE IsSignature(const byte *D,size_t Size);
    void UpdateLatestTime(FileHeader *CurBlock);
    void ConvertNameCase(char *Name);
#ifndef __BIONIC__
    void ConvertNameCase(wchar *Name);
#endif
    void ConvertUnknownHeader();
    size_t ReadOldHeader();
    void UnexpEndArcMsg();

#if !defined(SHELL_EXT) && !defined(RAR_NOCRYPT)
    CryptData HeadersCrypt;
    byte HeadersSalt[SALT_SIZE];
#endif
#ifndef SHELL_EXT
    ComprDataIO SubDataIO;
    byte SubDataSalt[SALT_SIZE];
#endif
    RAROptions *Cmd,DummyCmd;

    MarkHeader MarkHead;
    OldMainHeader OldMhd;

    int RecoverySectors;
    int64 RecoveryPos;

    bool FailedHeaderDecryption;

    RarTime LatestTime;
    int LastReadBlock;
    int CurHeaderType;

    bool SilentOpen;
  public:
    Archive(RAROptions *InitCmd=NULL);
    bool IsArchive(bool EnableBroken);
    size_t SearchBlock(int BlockType);
    size_t SearchSubBlock(const char *Type);
    int ReadBlock(int BlockType);
    void WriteBlock(int BlockType,BaseBlock *wb=NULL);
#ifndef __BIONIC__
    int PrepareNamesToWrite(char *Name,wchar *NameW,char *DestName,byte *DestNameW);
#else
    int PrepareNamesToWrite(char *Name,char *DestName);
#endif
    void SetLhdSize();
    size_t ReadHeader();
    void CheckArc(bool EnableBroken);
#ifndef __BIONIC__
    void CheckOpen(const char *Name,const wchar *NameW=NULL);
    bool WCheckOpen(const char *Name,const wchar *NameW=NULL);
    bool GetComment(Array<byte> *CmtData,Array<wchar> *CmtDataW);
#else
    void CheckOpen(const char *Name);
    bool WCheckOpen(const char *Name);
    bool GetComment(Array<byte> *CmtData);
#endif
    void ViewComment();
    void ViewFileComment();
    void SetLatestTime(RarTime *NewTime);
    void SeekToNext();
    bool CheckAccess();
    bool IsArcDir();
    bool IsArcLabel();
    void ConvertAttributes();
    int GetRecoverySize(bool Required);
    void VolSubtractHeaderSize(size_t SubSize);
    void AddSubData(byte *SrcData,size_t DataSize,File *SrcFile,const char *Name,bool AllowSplit);
    bool ReadSubData(Array<byte> *UnpData,File *DestFile);
    int GetHeaderType() {return(CurHeaderType);};
#ifndef __BIONIC__
    size_t ReadCommentData(Array<byte> *CmtData,Array<wchar> *CmtDataW);
#else
    size_t ReadCommentData(Array<byte> *CmtData);
#endif
    void WriteCommentData(byte *Data,size_t DataSize,bool FileComment);
    RAROptions* GetRAROptions() {return(Cmd);}
    void SetSilentOpen(bool Mode) {SilentOpen=Mode;}

    BaseBlock ShortBlock;
    MainHeader NewMhd;
    FileHeader NewLhd;
    EndArcHeader EndArcHead;
    SubBlockHeader SubBlockHead;
    FileHeader SubHead;
    CommentHeader CommHead;
    ProtectHeader ProtectHead;
    AVHeader AVHead;
    SignHeader SignHead;
    UnixOwnersHeader UOHead;
    MacFInfoHeader MACHead;
    EAHeader EAHead;
    StreamHeader StreamHead;

    int64 CurBlockPos;
    int64 NextBlockPos;

    bool OldFormat;
    bool Solid;
    bool Volume;
    bool MainComment;
    bool Locked;
    bool Signed;
    bool NotFirstVolume;
    bool Protected;
    bool Encrypted;
    size_t SFXSize;
    bool BrokenFileHeader;

    bool Splitting;

    ushort HeaderCRC;

    int64 VolWrite;
    int64 AddingFilesSize;
    size_t AddingHeadersSize;

    bool NewArchive;

    char FirstVolumeName[NM];
#ifndef __BIONIC__
    wchar FirstVolumeNameW[NM];
#endif
};


#endif

#ifndef _RAR_RECVOL_
#define _RAR_RECVOL_

class RecVolumes
{
  private:
    File *SrcFile[256];
    Array<byte> Buf;

#ifdef RAR_SMP
    ThreadPool RSThreadPool;
#endif
  public:
    RecVolumes();
    ~RecVolumes();
#ifndef __BIONIC__
    void Make(RAROptions *Cmd,char *ArcName,wchar *ArcNameW);
    bool Restore(RAROptions *Cmd,const char *Name,const wchar *NameW,bool Silent);
#else
    void Make(RAROptions *Cmd,char *ArcName);
    bool Restore(RAROptions *Cmd,const char *Name,bool Silent);
#endif
};

#endif

#ifndef _RAR_CONSIO_
#define _RAR_CONSIO_

#if !defined(SILENT) && !defined(SFX_MODULE)
  enum {SOUND_OK,SOUND_ALARM,SOUND_ERROR,SOUND_QUESTION};
#endif

enum PASSWORD_TYPE {PASSWORD_GLOBAL,PASSWORD_FILE,PASSWORD_ARCHIVE};

void InitConsoleOptions(MESSAGE_TYPE MsgStream,bool Sound);

#ifndef SILENT
  void mprintf(const char *fmt,...);
  void eprintf(const char *fmt,...);
  void Alarm();
#ifndef __BIONIC__
  void GetPasswordText(wchar *Str,uint MaxLength);
  bool GetPassword(PASSWORD_TYPE Type,const char *FileName,const wchar *FileNameW,SecPassword *Password);
#else
  void GetPasswordText(char *Str,uint MaxLength);
  bool GetPassword(PASSWORD_TYPE Type,const char *FileName,SecPassword *Password);
#endif
  int Ask(const char *AskStr);
#ifdef __BIONIC__
  const char *getpass(const char *prompt);
#endif
#endif

void OutComment(char *Comment,size_t Size);

#ifdef SILENT
  #ifdef __GNUC__
    #define mprintf(args...)
    #define eprintf(args...)
  #else
    inline void mprintf(const char *fmt,...) {}
    inline void eprintf(const char *fmt,...) {}
  #endif
  inline void Alarm() {}
#ifndef __BIONIC__
  inline void GetPasswordText(wchar *Str,uint MaxLength) {}
  inline bool GetPassword(PASSWORD_TYPE Type,const char *FileName,const wchar *FileNameW,SecPassword *Password) {return(false);}
#else
  inline void GetPasswordText(char *Str,uint MaxLength) {}
  inline bool GetPassword(PASSWORD_TYPE Type,const char *FileName,SecPassword *Password) {return(false);}
#endif
  inline int Ask(const char *AskStr) {return(0);}
#endif

#endif

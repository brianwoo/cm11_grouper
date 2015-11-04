#ifndef _RAR_CMDDATA_
#define _RAR_CMDDATA_


#define DefaultStoreList "7z;ace;arj;bz2;cab;gz;jpeg;jpg;lha;lzh;mp3;rar;taz;tgz;z;zip"

enum RAR_CMD_LIST_MODE {RCLM_AUTO,RCLM_REJECT_LISTS,RCLM_ACCEPT_LISTS};

class CommandData:public RAROptions
{
  private:
    void ProcessSwitchesString(char *Str);
#ifndef __BIONIC__
    void ProcessSwitch(const char *Switch,const wchar *SwitchW=NULL);
#else
    void ProcessSwitch(const char *Switch);
#endif
    void BadSwitch(const char *Switch);
    bool ExclCheckArgs(StringList *Args,bool Dir,char *CheckName,bool CheckFullPath,int MatchMode);
    uint GetExclAttr(const char *Str);

    bool FileLists;
    bool NoMoreSwitches;
    RAR_CMD_LIST_MODE ListMode;
    bool BareOutput;
  public:
    CommandData();
    ~CommandData();
    void Init();
    void Close();

    void PreprocessCommandLine(int argc, char *argv[]);
    void ParseCommandLine(int argc, char *argv[]);
#ifndef __BIONIC__
    void ParseArg(char *Arg,wchar *ArgW);
#else
    void ParseArg(char *Arg);
#endif
    void ParseDone();
    void ParseEnvVar();
    void ReadConfig();
    bool PreprocessSwitch(const char *Switch);
    void OutTitle();
    void OutHelp(RAR_EXIT ExitCode);
    bool IsSwitch(int Ch);
    bool ExclCheck(char *CheckName,bool Dir,bool CheckFullPath,bool CheckInclList);
    bool ExclDirByAttr(uint FileAttr);
    bool TimeCheck(RarTime &ft);
    bool SizeCheck(int64 Size);
    bool AnyFiltersActive();
    int IsProcessFile(FileHeader &NewLhd,bool *ExactMatch=NULL,int MatchType=MATCH_WILDSUBPATH);
    void ProcessCommand();
#ifndef __BIONIC__
    void AddArcName(const char *Name,const wchar *NameW);
    bool GetArcName(char *Name,wchar *NameW,int MaxSize);
#else
    void AddArcName(const char *Name);
    bool GetArcName(char *Name,int MaxSize);
#endif
    bool CheckWinSize();

    int GetRecoverySize(const char *Str,int DefSize);

    char Command[NM+16];
#ifndef __BIONIC__
    wchar CommandW[NM+16];
#endif

    char ArcName[NM];
#ifndef __BIONIC__
    wchar ArcNameW[NM];
#endif

    StringList *FileArgs;
    StringList *ExclArgs;
    StringList *InclArgs;
    StringList *ArcNames;
    StringList *StoreArgs;
};

#endif

#ifndef _RAR_EXTINFO_
#define _RAR_EXTINFO_

#ifndef __BIONIC__
void SetExtraInfo(CommandData *Cmd,Archive &Arc,char *Name,wchar *NameW);
void SetExtraInfoNew(CommandData *Cmd,Archive &Arc,char *Name,wchar *NameW);
#else
void SetExtraInfo(CommandData *Cmd,Archive &Arc,char *Name);
void SetExtraInfoNew(CommandData *Cmd,Archive &Arc,char *Name);
#endif

#endif

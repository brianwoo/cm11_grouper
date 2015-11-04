#include "rar.hpp"

StringList::StringList()
{
  Reset();
}


void StringList::Reset()
{
  Rewind();
  StringData.Reset();
#ifndef __BIONIC__
  StringDataW.Reset();
#endif
  StringsCount=0;
  SavePosNumber=0;
}

#ifndef __BIONIC__
void StringList::AddString(const char *Str)
{
  AddString(Str,NULL);
}


void StringList::AddString(const wchar *Str)
{
  AddString(NULL,Str);
}
#endif


#ifndef __BIONIC__
void StringList::AddString(const char *Str,const wchar *StrW)
#else
void StringList::AddString(const char *Str)
#endif
{
  if (Str==NULL)
    Str="";
#ifndef __BIONIC__
  if (StrW==NULL)
    StrW=L"";
#endif

  size_t PrevSize=StringData.Size();
  StringData.Add(strlen(Str)+1);
  strcpy(&StringData[PrevSize],Str);

#ifndef __BIONIC__
  size_t PrevSizeW=StringDataW.Size();
  StringDataW.Add(wcslen(StrW)+1);
  wcscpy(&StringDataW[PrevSizeW],StrW);
#endif

  StringsCount++;
}

#ifndef __BIONIC__
bool StringList::GetString(char *Str,size_t MaxLength)
{
  return(GetString(Str,NULL,MaxLength));
}

bool StringList::GetString(wchar *Str,size_t MaxLength)
{
  return(GetString(NULL,Str,MaxLength));
}
#endif

#ifndef __BIONIC__
bool StringList::GetString(char *Str,wchar *StrW,size_t MaxLength)
#else
bool StringList::GetString(char *Str,size_t MaxLength)
#endif
{
  char *StrPtr;
#ifndef __BIONIC__
  wchar *StrPtrW;
  if (!GetString(&StrPtr,&StrPtrW))
#else
  if (!GetString(&StrPtr))
#endif
    return(false);

  if (Str!=NULL)
    strncpy(Str,StrPtr,MaxLength);
#ifndef __BIONIC__
  if (StrW!=NULL)
    wcsncpy(StrW,StrPtrW,MaxLength);
#endif
  return(true);
}


#ifndef SFX_MODULE
#ifndef __BIONIC__
bool StringList::GetString(char *Str,wchar *StrW,size_t MaxLength,int StringNum)
#else
bool StringList::GetString(char *Str,size_t MaxLength,int StringNum)
#endif
{
  SavePosition();
  Rewind();
  bool RetCode=true;
  while (StringNum-- >=0)
#ifndef __BIONIC__
    if (!GetString(Str,StrW,MaxLength))
#else
    if (!GetString(Str,MaxLength))
#endif
    {
      RetCode=false;
      break;
    }
  RestorePosition();
  return(RetCode);
}
#endif


char* StringList::GetString()
{
  char *Str;
#ifndef __BIONIC__
  GetString(&Str,NULL);
#else
  GetString(&Str);
#endif
  return(Str);
}

#ifndef __BIONIC__
wchar* StringList::GetStringW()
{
  wchar *StrW;
  GetString(NULL,&StrW);
  return(StrW);
}
#endif

#ifndef __BIONIC__
bool StringList::GetString(char **Str,wchar **StrW)
#else
bool StringList::GetString(char **Str)
#endif
{
  // First check would be enough, because both buffers grow synchronously,
  // but we check both for extra fail proof.
#ifndef __BIONIC__
  if (CurPos>=StringData.Size() || CurPosW>=StringDataW.Size())
#else
  if (CurPos>=StringData.Size())
#endif
  {
    // No more strings left unprocessed.
    if (Str!=NULL)
      *Str=NULL;
#ifndef __BIONIC__
    if (StrW!=NULL)
      *StrW=NULL;
#endif
    return(false);
  }

  // We move ASCII and Unicode buffer pointers synchronously.
  
  char *CurStr=&StringData[CurPos];
  CurPos+=strlen(CurStr)+1;
  if (Str!=NULL)
    *Str=CurStr;

#ifndef __BIONIC__
  wchar *CurStrW=&StringDataW[CurPosW];
  CurPosW+=wcslen(CurStrW)+1;
  if (StrW!=NULL)
    *StrW=CurStrW;
#endif

  return(true);
}


void StringList::Rewind()
{
  CurPos=0;
#ifndef __BIONIC__
  CurPosW=0;
#endif
}


// Return the total size of usual and Unicode characters stored in the list.
size_t StringList::GetCharCount()
{
#ifndef __BIONIC__
  return(StringData.Size()+StringDataW.Size());
#else
  return(StringData.Size());
#endif
}


#ifndef SFX_MODULE
#ifndef __BIONIC__
bool StringList::Search(char *Str,wchar *StrW,bool CaseSensitive)
#else
bool StringList::Search(char *Str,bool CaseSensitive)
#endif
{
  SavePosition();
  Rewind();
  bool Found=false;
  char *CurStr;
#ifndef __BIONIC__
  wchar *CurStrW;
  while (GetString(&CurStr,&CurStrW))
#else
  while (GetString(&CurStr))
#endif
  {
    if (Str!=NULL && CurStr!=NULL)
      if ((CaseSensitive ? strcmp(Str,CurStr):stricomp(Str,CurStr))!=0)
        continue;
#ifndef __BIONIC__
    if (StrW!=NULL && CurStrW!=NULL)
      if ((CaseSensitive ? wcscmp(StrW,CurStrW):wcsicomp(StrW,CurStrW))!=0)
        continue;
#endif
    Found=true;
    break;
  }
  RestorePosition();
  return(Found);
}
#endif


#ifndef SFX_MODULE
void StringList::SavePosition()
{
  if (SavePosNumber<ASIZE(SaveCurPos))
  {
    SaveCurPos[SavePosNumber]=CurPos;
#ifndef __BIONIC__
    SaveCurPosW[SavePosNumber]=CurPosW;
#endif
    SavePosNumber++;
  }
}
#endif


#ifndef SFX_MODULE
void StringList::RestorePosition()
{
  if (SavePosNumber>0)
  {
    SavePosNumber--;
    CurPos=SaveCurPos[SavePosNumber];
#ifndef __BIONIC__
    CurPosW=SaveCurPosW[SavePosNumber];
#endif
  }
}
#endif

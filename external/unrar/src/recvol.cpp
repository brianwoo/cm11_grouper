#include "rar.hpp"

// Buffer size for all volumes involved.
static const size_t TotalBufferSize=0x4000000;

class RSEncode // Encode or decode data area, one object per one thread.
{
  private:
    RSCoder RSC;
  public:
    void EncodeBuf();
    void DecodeBuf();

    void Init(int RecVolNumber) {RSC.Init(RecVolNumber);}
    byte *Buf;
    byte *OutBuf;
    int BufStart;
    int BufEnd;
    int FileNumber;
    int RecVolNumber;
    size_t RecBufferSize;
    int *Erasures;
    int EraSize;
};


#ifdef RAR_SMP
THREAD_PROC(RSEncodeThread)
{
  RSEncode *rs=(RSEncode *)Data;
  rs->EncodeBuf();
}

THREAD_PROC(RSDecodeThread)
{
  RSEncode *rs=(RSEncode *)Data;
  rs->DecodeBuf();
}
#endif

RecVolumes::RecVolumes()
{
  Buf.Alloc(TotalBufferSize);
  memset(SrcFile,0,sizeof(SrcFile));
}


RecVolumes::~RecVolumes()
{
  for (int I=0;I<ASIZE(SrcFile);I++)
    delete SrcFile[I];
}




void RSEncode::EncodeBuf()
{
  for (int BufPos=BufStart;BufPos<BufEnd;BufPos++)
  {
    byte Data[256],Code[256];
    for (int I=0;I<FileNumber;I++)
      Data[I]=Buf[I*RecBufferSize+BufPos];
    RSC.Encode(Data,FileNumber,Code);
    for (int I=0;I<RecVolNumber;I++)
      OutBuf[I*RecBufferSize+BufPos]=Code[I];
  }
}

#ifndef __BIONIC__
bool RecVolumes::Restore(RAROptions *Cmd,const char *Name,
                         const wchar *NameW,bool Silent)
#else
bool RecVolumes::Restore(RAROptions *Cmd,const char *Name,bool Silent)
#endif
{
  char ArcName[NM];
#ifndef __BIONIC__
  wchar ArcNameW[NM];
#endif
  strcpy(ArcName,Name);
#ifndef __BIONIC__
  wcscpy(ArcNameW,NameW);
#endif
  char *Ext=GetExt(ArcName);
  bool NewStyle=false;
  bool RevName=Ext!=NULL && stricomp(Ext,".rev")==0;
  if (RevName)
  {
    for (int DigitGroup=0;Ext>ArcName && DigitGroup<3;Ext--)
      if (!IsDigit(*Ext))
        if (IsDigit(*(Ext-1)) && (*Ext=='_' || DigitGroup<2))
          DigitGroup++;
        else
          if (DigitGroup<2)
          {
            NewStyle=true;
            break;
          }
    while (IsDigit(*Ext) && Ext>ArcName+1)
      Ext--;
    strcpy(Ext,"*.*");

#ifndef __BIONIC__
    if (*ArcNameW!=0)
    {
      wchar *ExtW=GetExt(ArcNameW);
      for (int DigitGroup=0;ExtW>ArcNameW && DigitGroup<3;ExtW--)
        if (!IsDigit(*ExtW))
          if (IsDigit(*(ExtW-1)) && (*ExtW=='_' || DigitGroup<2))
            DigitGroup++;
          else
            if (DigitGroup<2)
            {
              NewStyle=true;
              break;
            }
      while (IsDigit(*ExtW) && ExtW>ArcNameW+1)
        ExtW--;
      wcscpy(ExtW,L"*.*");
    }
#endif
    
    FindFile Find;
    Find.SetMask(ArcName);
#ifndef __BIONIC__
    Find.SetMaskW(ArcNameW);
#endif
    FindData fd;
    while (Find.Next(&fd))
    {
      Archive Arc(Cmd);
#ifndef __BIONIC__
      if (Arc.WOpen(fd.Name,fd.NameW) && Arc.IsArchive(true))
#else
      if (Arc.WOpen(fd.Name) && Arc.IsArchive(true))
#endif
      {
        strcpy(ArcName,fd.Name);
#ifndef __BIONIC__
        wcscpy(ArcNameW,fd.NameW);
#endif
        break;
      }
    }
  }

  Archive Arc(Cmd);
#ifndef __BIONIC__
  if (!Arc.WCheckOpen(ArcName,ArcNameW))
#else
  if (!Arc.WCheckOpen(ArcName))
#endif
    return(false);
  if (!Arc.Volume)
  {
#ifndef SILENT
    Log(ArcName,St(MNotVolume),ArcName);
#endif
    return(false);
  }
  bool NewNumbering=(Arc.NewMhd.Flags & MHD_NEWNUMBERING)!=0;
  Arc.Close();

  char *VolNumStart=VolNameToFirstName(ArcName,ArcName,NewNumbering);
  char RecVolMask[NM];
  strcpy(RecVolMask,ArcName);
  size_t BaseNamePartLength=VolNumStart-ArcName;
  strcpy(RecVolMask+BaseNamePartLength,"*.rev");

  wchar RecVolMaskW[NM];
  size_t BaseNamePartLengthW=0;
  *RecVolMaskW=0;
#ifndef __BIONIC__
  if (*ArcNameW!=0)
  {
    wchar *VolNumStartW=VolNameToFirstName(ArcNameW,ArcNameW,NewNumbering);
    wcscpy(RecVolMaskW,ArcNameW);
    BaseNamePartLengthW=VolNumStartW-ArcNameW;
    wcscpy(RecVolMaskW+BaseNamePartLengthW,L"*.rev");
  }
#endif


#ifndef SILENT
  int64 RecFileSize=0;
#endif

  // We cannot display "Calculating CRC..." message here, because we do not
  // know if we'll find any recovery volumes. We'll display it after finding
  // the first recovery volume.
  bool CalcCRCMessageDone=false;

  FindFile Find;
  Find.SetMask(RecVolMask);
#ifndef __BIONIC__
  Find.SetMaskW(RecVolMaskW);
#endif
  FindData RecData;
  int FileNumber=0,RecVolNumber=0,FoundRecVolumes=0,MissingVolumes=0;
  char PrevName[NM];
#ifndef __BIONIC__
  wchar PrevNameW[NM];
#endif
  while (Find.Next(&RecData))
  {
    char *CurName=RecData.Name;
#ifndef __BIONIC__
    wchar *CurNameW=RecData.NameW;
#endif
    int P[3];
    if (!RevName && !NewStyle)
    {
      NewStyle=true;

      char *Dot=GetExt(CurName);
      if (Dot!=NULL)
      {
        int LineCount=0;
        Dot--;
        while (Dot>CurName && *Dot!='.')
        {
          if (*Dot=='_')
            LineCount++;
          Dot--;
        }
        if (LineCount==2)
          NewStyle=false;
      }

#ifndef __BIONIC__
      wchar *DotW=GetExt(CurNameW);
      if (DotW!=NULL)
      {
        int LineCount=0;
        DotW--;
        while (DotW>CurNameW && *DotW!='.')
        {
          if (*DotW=='_')
            LineCount++;
          DotW--;
        }
        if (LineCount==2)
          NewStyle=false;
      }
#endif
    }
    if (NewStyle)
    {
      if (!CalcCRCMessageDone)
      {
#ifndef SILENT
        mprintf(St(MCalcCRCAllVol));
#endif
        CalcCRCMessageDone=true;
      }
      
#ifndef SILENT
        mprintf("\r\n%s",CurName);
#endif

      File CurFile;
#ifndef __BIONIC__
      CurFile.TOpen(CurName,CurNameW);
#else
      CurFile.TOpen(CurName);
#endif
      CurFile.Seek(0,SEEK_END);
      int64 Length=CurFile.Tell();
      CurFile.Seek(Length-7,SEEK_SET);
      for (int I=0;I<3;I++)
        P[2-I]=CurFile.GetByte()+1;
      uint FileCRC=0;
      for (int I=0;I<4;I++)
        FileCRC|=CurFile.GetByte()<<(I*8);
      if (FileCRC!=CalcFileCRC(&CurFile,Length-4))
      {
#ifndef SILENT
        mprintf(St(MCRCFailed),CurName);
#endif
        continue;
      }
    }
    else
    {
      char *Dot=GetExt(CurName);
      if (Dot==NULL)
        continue;
      bool WrongParam=false;
      for (int I=0;I<ASIZE(P);I++)
      {
        do
        {
          Dot--;
        } while (IsDigit(*Dot) && Dot>=CurName+BaseNamePartLength);
        P[I]=atoi(Dot+1);
        if (P[I]==0 || P[I]>255)
          WrongParam=true;
      }
      if (WrongParam)
        continue;
    }
    if (P[1]+P[2]>255)
      continue;
    if (RecVolNumber!=0 && RecVolNumber!=P[1] || FileNumber!=0 && FileNumber!=P[2])
    {
#ifndef SILENT
      Log(NULL,St(MRecVolDiffSets),CurName,PrevName);
#endif
      return(false);
    }
    RecVolNumber=P[1];
    FileNumber=P[2];
    strcpy(PrevName,CurName);
#ifndef __BIONIC__
    wcscpy(PrevNameW,CurNameW);
#endif
    File *NewFile=new File;
#ifndef __BIONIC__
    NewFile->TOpen(CurName,CurNameW);
#else
    NewFile->TOpen(CurName);
#endif
    SrcFile[FileNumber+P[0]-1]=NewFile;
    FoundRecVolumes++;
#ifndef SILENT
    if (RecFileSize==0)
      RecFileSize=NewFile->FileLength();
#endif
  }
#ifndef SILENT
  if (!Silent || FoundRecVolumes!=0)
  {
    mprintf(St(MRecVolFound),FoundRecVolumes);
  }
#endif
  if (FoundRecVolumes==0)
    return(false);

  bool WriteFlags[256];
  memset(WriteFlags,0,sizeof(WriteFlags));

  char LastVolName[NM];
  *LastVolName=0;
#ifndef __BIONIC__
  wchar LastVolNameW[NM];
  *LastVolNameW=0;
#endif

  for (int CurArcNum=0;CurArcNum<FileNumber;CurArcNum++)
  {
    Archive *NewFile=new Archive;
#ifndef __BIONIC__
    bool ValidVolume=FileExist(ArcName,ArcNameW);
#else
    bool ValidVolume=FileExist(ArcName);
#endif
    if (ValidVolume)
    {
#ifndef __BIONIC__
      NewFile->TOpen(ArcName,ArcNameW);
#else
      NewFile->TOpen(ArcName);
#endif
      ValidVolume=NewFile->IsArchive(false);
      if (ValidVolume)
      {
        while (NewFile->ReadHeader()!=0)
        {
          if (NewFile->GetHeaderType()==ENDARC_HEAD)
          {
#ifndef SILENT
            mprintf("\r\n%s",ArcName);
#endif
            if ((NewFile->EndArcHead.Flags&EARC_DATACRC)!=0 && 
                NewFile->EndArcHead.ArcDataCRC!=CalcFileCRC(NewFile,NewFile->CurBlockPos))
            {
              ValidVolume=false;
#ifndef SILENT
              mprintf(St(MCRCFailed),ArcName);
#endif
            }
            break;
          }
          NewFile->SeekToNext();
        }
      }
      if (!ValidVolume)
      {
        NewFile->Close();
        char NewName[NM];
        strcpy(NewName,ArcName);
        strcat(NewName,".bad");

#ifndef __BIONIC__
        wchar NewNameW[NM];
        wcscpy(NewNameW,ArcNameW);
        if (*NewNameW!=0)
          wcscat(NewNameW,L".bad");
#endif
#ifndef SILENT
        mprintf(St(MBadArc),ArcName);
        mprintf(St(MRenaming),ArcName,NewName);
#endif
#ifndef __BIONIC__
        RenameFile(ArcName,ArcNameW,NewName,NewNameW);
#else
        RenameFile(ArcName,NewName);
#endif
      }
      NewFile->Seek(0,SEEK_SET);
    }
    if (!ValidVolume)
    {
      // It is important to return 'false' instead of aborting here,
      // so if we are called from extraction, we will be able to continue
      // extracting. It may happen if .rar and .rev are on read-only disks
      // like CDs.
#ifndef __BIONIC__
      if (!NewFile->Create(ArcName,ArcNameW))
#else
      if (!NewFile->Create(ArcName))
#endif
      {
        // We need to display the title of operation before the error message,
        // to make clear for user that create error is related to recovery 
        // volumes. This is why we cannot use WCreate call here. Title must be
        // before create error, not after that.
#ifndef SILENT
        mprintf(St(MReconstructing));
#endif
#ifndef __BIONIC__
        ErrHandler.CreateErrorMsg(ArcName,ArcNameW);
#else
        ErrHandler.CreateErrorMsg(ArcName);
#endif
        return false;
      }

      WriteFlags[CurArcNum]=true;
      MissingVolumes++;

      if (CurArcNum==FileNumber-1)
      {
        strcpy(LastVolName,ArcName);
#ifndef __BIONIC__
        wcscpy(LastVolNameW,ArcNameW);
#endif
      }

#ifndef SILENT
      mprintf(St(MAbsNextVol),ArcName);
#endif
    }
    SrcFile[CurArcNum]=(File*)NewFile;
#ifndef __BIONIC__
    NextVolumeName(ArcName,ArcNameW,ASIZE(ArcName),!NewNumbering);
#else
    NextVolumeName(ArcName,ASIZE(ArcName),!NewNumbering);
#endif
  }

#ifndef SILENT
  mprintf(St(MRecVolMissing),MissingVolumes);
#endif

  if (MissingVolumes==0)
  {
#ifndef SILENT
    mprintf(St(MRecVolAllExist));
#endif
    return(false);
  }

  if (MissingVolumes>FoundRecVolumes)
  {
#ifndef SILENT
    mprintf(St(MRecVolCannotFix));
#endif
    return(false);
  }
#ifndef SILENT
  mprintf(St(MReconstructing));
#endif

  int TotalFiles=FileNumber+RecVolNumber;
  int Erasures[256],EraSize=0;

  for (int I=0;I<TotalFiles;I++)
    if (WriteFlags[I] || SrcFile[I]==NULL)
      Erasures[EraSize++]=I;

#ifndef SILENT
  int64 ProcessedSize=0;
#ifndef GUI
  int LastPercent=-1;
  mprintf("     ");
#endif
#endif
  // Size of per file buffer.
  size_t RecBufferSize=TotalBufferSize/TotalFiles;

#ifdef RAR_SMP
  uint ThreadNumber=Cmd->Threads;
  RSEncode rse[MaxPoolThreads];
  uint WaitHandles[MaxPoolThreads];
#else
  uint ThreadNumber=1;
  RSEncode rse[1];
#endif
  for (uint I=0;I<ThreadNumber;I++)
    rse[I].Init(RecVolNumber);

  while (true)
  {
    Wait();
    int MaxRead=0;
    for (int I=0;I<TotalFiles;I++)
      if (WriteFlags[I] || SrcFile[I]==NULL)
        memset(&Buf[I*RecBufferSize],0,RecBufferSize);
      else
      {
        int ReadSize=SrcFile[I]->Read(&Buf[I*RecBufferSize],RecBufferSize);
        if (ReadSize!=RecBufferSize)
          memset(&Buf[I*RecBufferSize+ReadSize],0,RecBufferSize-ReadSize);
        if (ReadSize>MaxRead)
          MaxRead=ReadSize;
      }
    if (MaxRead==0)
      break;
#ifndef SILENT
    int CurPercent=ToPercent(ProcessedSize,RecFileSize);
    if (!Cmd->DisablePercentage && CurPercent!=LastPercent)
    {
      mprintf("\b\b\b\b%3d%%",CurPercent);
      LastPercent=CurPercent;
    }
    ProcessedSize+=MaxRead;
#endif
   

    int BlockStart=0;
    int BlockSize=MaxRead/ThreadNumber;
    if (BlockSize<0x100)
      BlockSize=MaxRead;
    uint CurThread=0;

    while (BlockStart<MaxRead)
    {
      // Last thread processes all left data including increasement
      // from rounding error.
      if (CurThread==ThreadNumber-1)
        BlockSize=MaxRead-BlockStart;

      RSEncode *curenc=rse+CurThread;
      curenc->Buf=&Buf[0];
      curenc->BufStart=BlockStart;
      curenc->BufEnd=BlockStart+BlockSize;
      curenc->FileNumber=TotalFiles;
      curenc->RecBufferSize=RecBufferSize;
      curenc->Erasures=Erasures;
      curenc->EraSize=EraSize;

#ifdef RAR_SMP
      if (ThreadNumber>1)
      {
        uint Handle=RSThreadPool.Start(RSDecodeThread,(void*)curenc);
        WaitHandles[CurThread++]=Handle;
      }
      else
        curenc->DecodeBuf();
#else
      curenc->DecodeBuf();
#endif

      BlockStart+=BlockSize;
    }

#ifdef RAR_SMP
    if (CurThread>0)
      RSThreadPool.Wait(WaitHandles,CurThread);
#endif // RAR_SMP
    
    for (int I=0;I<FileNumber;I++)
      if (WriteFlags[I])
        SrcFile[I]->Write(&Buf[I*RecBufferSize],MaxRead);
  }
  for (int I=0;I<RecVolNumber+FileNumber;I++)
    if (SrcFile[I]!=NULL)
    {
      File *CurFile=SrcFile[I];
      if (NewStyle && WriteFlags[I])
      {
        int64 Length=CurFile->Tell();
        CurFile->Seek(Length-7,SEEK_SET);
        for (int J=0;J<7;J++)
          CurFile->PutByte(0);
      }
      CurFile->Close();
      SrcFile[I]=NULL;
    }
#ifndef __BIONIC__
  if (*LastVolName!=0 || *LastVolNameW!=0)
#else
  if (*LastVolName!=0)
#endif
  {
    // Truncate the last volume to its real size.
    Archive Arc(Cmd);
#ifndef __BIONIC__
    if (Arc.Open(LastVolName,LastVolNameW,FMF_UPDATE) && Arc.IsArchive(true) &&
#else
    if (Arc.Open(LastVolName,FMF_UPDATE) && Arc.IsArchive(true) &&
#endif
        Arc.SearchBlock(ENDARC_HEAD))
    {
      Arc.Seek(Arc.NextBlockPos,SEEK_SET);
      char Buf[8192];
      int ReadSize=Arc.Read(Buf,sizeof(Buf));
      int ZeroCount=0;
      while (ZeroCount<ReadSize && Buf[ZeroCount]==0)
        ZeroCount++;
      if (ZeroCount==ReadSize)
      {
        Arc.Seek(Arc.NextBlockPos,SEEK_SET);
        Arc.Truncate();
      }
    }
  }
#if !defined(GUI) && !defined(SILENT)
  if (!Cmd->DisablePercentage)
    mprintf("\b\b\b\b100%%");
  if (!Silent && !Cmd->DisableDone)
    mprintf(St(MDone));
#endif
  return(true);
}


void RSEncode::DecodeBuf()
{
  for (int BufPos=BufStart;BufPos<BufEnd;BufPos++)
  {
    byte Data[256];
    for (int I=0;I<FileNumber;I++)
      Data[I]=Buf[I*RecBufferSize+BufPos];
    RSC.Decode(Data,FileNumber,Erasures,EraSize);
    for (int I=0;I<EraSize;I++)
      Buf[Erasures[I]*RecBufferSize+BufPos]=Data[Erasures[I]];
  }
}

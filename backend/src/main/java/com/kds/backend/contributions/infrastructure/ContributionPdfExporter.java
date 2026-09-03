package com.kds.backend.contributions.infrastructure;

import com.kds.backend.contributions.application.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Component;
import java.io.*;
import java.math.BigDecimal;
import java.util.List;

@Component
public class ContributionPdfExporter implements ContributionReportExporter {
    private static final float MARGIN=42,LINE=16;
    @Override public ContributionReportFormat format(){return ContributionReportFormat.PDF;}
    @Override public String mediaType(){return "application/pdf";}
    @Override public String extension(){return "pdf";}
    @Override public void write(ContributionReport report,OutputStream output)throws IOException{
        try(PDDocument document=new PDDocument()){
            PDType1Font regular=new PDType1Font(Standard14Fonts.FontName.HELVETICA),bold=new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            int start=0; do {start=page(document,report,start,regular,bold);} while(start<report.members().size()); document.save(output);
        }
    }
    private int page(PDDocument document,ContributionReport report,int start,PDType1Font regular,PDType1Font bold)throws IOException{
        PDPage page=new PDPage(PDRectangle.LETTER);document.addPage(page);float y=page.getMediaBox().getHeight()-MARGIN;
        try(PDPageContentStream content=new PDPageContentStream(document,page)){
            text(content,bold,16,MARGIN,y,ascii(bold,report.clubName()+" - Contribution report"));y-=24;
            text(content,regular,10,MARGIN,y,"Period: "+report.from()+" to "+report.to());y-=LINE;
            text(content,regular,10,MARGIN,y,"Expected: "+money(report.totalExpected())+"   Collected: "+money(report.totalCollected())+"   Outstanding: "+money(report.totalOutstanding())+" "+report.currency());y-=24;
            headings(content,bold,y);y-=LINE;int index=start;
            while(index<report.members().size()&&y>MARGIN+LINE){row(content,regular,y,report.members().get(index));y-=LINE;index++;}
            text(content,regular,8,MARGIN,22,"Generated "+report.generatedAt());return index;
        }
    }
    private void headings(PDPageContentStream content,PDType1Font font,float y)throws IOException{text(content,font,9,MARGIN,y,"Member");text(content,font,9,270,y,"Expected");text(content,font,9,360,y,"Collected");text(content,font,9,455,y,"Outstanding");}
    private void row(PDPageContentStream content,PDType1Font font,float y,MemberContributionSummary member)throws IOException{text(content,font,8,MARGIN,y,clip(ascii(font,member.memberName()+" <"+member.memberEmail()+">"),52));text(content,font,8,270,y,money(member.expected()));text(content,font,8,360,y,money(member.collected()));text(content,font,8,455,y,money(member.outstanding()));}
    private static void text(PDPageContentStream content,PDFont font,float size,float x,float y,String value)throws IOException{content.beginText();content.setFont(font,size);content.newLineAtOffset(x,y);content.showText(value);content.endText();}
    private static String ascii(PDFont font,String value)throws IOException{StringBuilder safe=new StringBuilder();for(int i=0;i<value.length();i++){String character=String.valueOf(value.charAt(i));try{font.encode(character);safe.append(character);}catch(IllegalArgumentException exception){safe.append('?');}}return safe.toString();}
    private static String clip(String value,int length){return value.length()<=length?value:value.substring(0,length-3)+"...";}
    private static String money(BigDecimal value){return "R "+value.setScale(2).toPlainString();}
}

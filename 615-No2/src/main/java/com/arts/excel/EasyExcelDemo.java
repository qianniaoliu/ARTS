package com.arts.excel;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import sun.misc.Unsafe;

import java.io.IOException;

/**
 * @author yusheng
 */
public class EasyExcelDemo {

    public static void main(String[] args) throws IOException {
        Workbook workbook = new XSSFWorkbook("C:\\Users\\cdshenlong1\\Desktop\\test.xlsx");
        EasyExcel.read("C:\\Users\\cdshenlong1\\Desktop\\test.xlsx")
                .registerReadListener(new AnalysisEventListener() {
                    @Override
                    public void invoke(Object o, AnalysisContext analysisContext) {
                        System.out.println(o);
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {

                    }
                })
                .doReadAll();

        System.out.println(11);
    }
}

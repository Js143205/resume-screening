import zipfile
from pathlib import Path
from xml.sax.saxutils import escape

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = Path(__file__).resolve().parent
DIAGRAM_DIR = OUT_DIR / "generated-diagrams"
OUTPUT_FILE = ROOT / "Synopsis_2_GNDU_Final_Resume_Screening.docx"
EMU_PER_PX = 9525


def para(text="", style=None, align=None, bold=False, underline=False, italic=False, spacing_before=0, spacing_after=120):
    text = "" if text is None else str(text)
    ppr = []
    if style:
        ppr.append(f'<w:pStyle w:val="{style}"/>')
    if align:
        ppr.append(f'<w:jc w:val="{align}"/>')
    ppr.append(f'<w:spacing w:before="{spacing_before}" w:after="{spacing_after}" w:line="360" w:lineRule="auto"/>')
    ppr_xml = f"<w:pPr>{''.join(ppr)}</w:pPr>"
    rpr = []
    if bold:
        rpr.append("<w:b/>")
    if underline:
        rpr.append('<w:u w:val="single"/>')
    if italic:
        rpr.append("<w:i/>")
    rpr_xml = f"<w:rPr>{''.join(rpr)}</w:rPr>" if rpr else ""
    return f"<w:p>{ppr_xml}<w:r>{rpr_xml}<w:t xml:space=\"preserve\">{escape(text)}</w:t></w:r></w:p>"


def blank_para():
    return '<w:p><w:r><w:t xml:space="preserve"> </w:t></w:r></w:p>'


def page_break_para():
    return '<w:p><w:r><w:br w:type="page"/></w:r></w:p>'


def code_block(lines):
    runs = ['<w:p><w:pPr><w:pStyle w:val="CodeBlock"/></w:pPr>']
    for idx, line in enumerate(lines):
        runs.append(f'<w:r><w:t xml:space="preserve">{escape(line)}</w:t></w:r>')
        if idx < len(lines) - 1:
            runs.append("<w:r><w:br/></w:r>")
    runs.append("</w:p>")
    return "".join(runs)


def image_para(rid, name, width_px, height_px, caption):
    cx = width_px * EMU_PER_PX
    cy = height_px * EMU_PER_PX
    xml = f"""
    <w:p>
      <w:pPr><w:jc w:val="center"/><w:spacing w:before="120" w:after="120"/></w:pPr>
      <w:r>
        <w:drawing>
          <wp:inline distT="0" distB="0" distL="0" distR="0"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
            <wp:extent cx="{cx}" cy="{cy}"/>
            <wp:docPr id="{image_para.counter}" name="{escape(name)}"/>
            <wp:cNvGraphicFramePr/>
            <a:graphic>
              <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                <pic:pic>
                  <pic:nvPicPr>
                    <pic:cNvPr id="0" name="{escape(name)}"/>
                    <pic:cNvPicPr/>
                  </pic:nvPicPr>
                  <pic:blipFill>
                    <a:blip r:embed="{rid}"/>
                    <a:stretch><a:fillRect/></a:stretch>
                  </pic:blipFill>
                  <pic:spPr>
                    <a:xfrm><a:off x="0" y="0"/><a:ext cx="{cx}" cy="{cy}"/></a:xfrm>
                    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                  </pic:spPr>
                </pic:pic>
              </a:graphicData>
            </a:graphic>
          </wp:inline>
        </w:drawing>
      </w:r>
    </w:p>
    """
    image_para.counter += 1
    return xml + para(caption, style="Caption", align="center", spacing_after=180)


image_para.counter = 1


SECTIONS = []


def add_heading(title, level=1):
    SECTIONS.append(para(title, style="Heading1Custom" if level == 1 else "Heading2Custom"))


def add_paragraph(text):
    SECTIONS.append(para(text))


def add_captioned_image(rid, name, width_px, height_px, caption):
    SECTIONS.append(image_para(rid, name, width_px, height_px, caption))


def build_body():
    add_paragraph("")
    SECTIONS.insert(0, page_break_para())

